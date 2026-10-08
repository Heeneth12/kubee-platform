package com.kubee.pos.ordering.infrastructure.persistence;

import com.kubee.pos.common.tenant.TenantContext;
import com.kubee.pos.ordering.application.port.CatalogPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads the catalog tables directly (the modules share one database). Keeps ordering independent of
 * catalog classes; if catalog moves out, only this adapter changes.
 */
@Component
@RequiredArgsConstructor
class JdbcCatalogPort implements CatalogPort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Optional<SellableItem> findItem(String itemUuid) {
        var params = new MapSqlParameterSource("tenant", TenantContext.requireTenantUuid()).addValue("uuid", itemUuid);
        List<SellableItem> items = jdbc.query("""
                SELECT i.id, i.uuid, i.name, i.hsn_sac_code, i.unit_of_measure, i.selling_price, i.price_includes_tax,
                       tg.id AS tax_group_id, COALESCE(tg.total_rate, 0) AS tax_rate,
                       i.has_variants, i.is_open_price, i.is_active
                FROM pos.items i
                LEFT JOIN pos.tax_groups tg ON tg.id = i.tax_group_id AND tg.is_deleted = false
                WHERE i.tenant_uuid = :tenant AND i.uuid = :uuid AND i.is_deleted = false
                """, params, (rs, n) -> {
            long id = rs.getLong("id");
            return new SellableItem(id, rs.getString("uuid"), rs.getString("name"), rs.getString("hsn_sac_code"),
                    rs.getString("unit_of_measure"), rs.getBigDecimal("selling_price"),
                    rs.getBoolean("price_includes_tax"), nullableLong(rs, "tax_group_id"), rs.getBigDecimal("tax_rate"),
                    rs.getBoolean("has_variants"), rs.getBoolean("is_open_price"), rs.getBoolean("is_active"),
                    null, null);
        });
        if (items.isEmpty()) {
            return Optional.empty();
        }
        SellableItem item = items.getFirst();
        params.addValue("itemId", item.id());
        return Optional.of(new SellableItem(item.id(), item.uuid(), item.name(), item.hsnSacCode(),
                item.unitOfMeasure(), item.sellingPrice(), item.priceIncludesTax(), item.taxGroupId(), item.taxRate(),
                item.hasVariants(), item.openPrice(), item.active(), variants(params), addonGroups(params)));
    }

    private List<SellableVariant> variants(MapSqlParameterSource params) {
        return jdbc.query("""
                SELECT id, uuid, name, selling_price, is_active FROM pos.item_variants
                WHERE item_id = :itemId AND tenant_uuid = :tenant AND is_deleted = false
                ORDER BY sort_order, id
                """, params, (rs, n) -> new SellableVariant(rs.getLong("id"), rs.getString("uuid"),
                rs.getString("name"), rs.getBigDecimal("selling_price"), rs.getBoolean("is_active")));
    }

    private List<SellableAddonGroup> addonGroups(MapSqlParameterSource params) {
        record Row(String groupUuid, String groupName, int min, Integer max, boolean groupActive, SellableAddon addon) {
        }
        List<Row> rows = jdbc.query("""
                SELECT g.uuid AS g_uuid, g.name AS g_name, g.min_select, g.max_select, g.is_active AS g_active,
                       a.id AS a_id, a.uuid AS a_uuid, a.name AS a_name, a.price AS a_price, a.is_active AS a_active
                FROM pos.item_addon_groups l
                JOIN pos.addon_groups g ON g.id = l.addon_group_id AND g.is_deleted = false
                LEFT JOIN pos.addons a ON a.addon_group_id = g.id AND a.is_deleted = false
                WHERE l.item_id = :itemId AND l.tenant_uuid = :tenant AND l.is_deleted = false
                ORDER BY l.sort_order, l.id, a.sort_order, a.id
                """, params, (rs, n) -> new Row(rs.getString("g_uuid"), rs.getString("g_name"), rs.getInt("min_select"),
                nullableInt(rs, "max_select"), rs.getBoolean("g_active"),
                rs.getString("a_uuid") == null ? null : new SellableAddon(rs.getLong("a_id"), rs.getString("a_uuid"),
                        rs.getString("a_name"), rs.getBigDecimal("a_price"), rs.getBoolean("a_active"))));

        Map<String, List<SellableAddon>> addonsByGroup = new LinkedHashMap<>();
        Map<String, Row> groups = new LinkedHashMap<>();
        for (Row row : rows) {
            groups.putIfAbsent(row.groupUuid(), row);
            List<SellableAddon> addons = addonsByGroup.computeIfAbsent(row.groupUuid(), k -> new ArrayList<>());
            if (row.addon() != null) {
                addons.add(row.addon());
            }
        }
        return groups.values().stream()
                .map(g -> new SellableAddonGroup(g.groupUuid(), g.groupName(), g.min(), g.max(), g.groupActive(),
                        List.copyOf(addonsByGroup.get(g.groupUuid()))))
                .toList();
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}
