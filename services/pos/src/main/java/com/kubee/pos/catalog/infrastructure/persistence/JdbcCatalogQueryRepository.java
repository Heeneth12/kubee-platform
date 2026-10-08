package com.kubee.pos.catalog.infrastructure.persistence;

import com.kubee.pos.catalog.application.query.AddonGroupView;
import com.kubee.pos.catalog.application.query.AddonView;
import com.kubee.pos.catalog.application.query.CatalogQueryRepository;
import com.kubee.pos.catalog.application.query.CategoryView;
import com.kubee.pos.catalog.application.query.ItemLookupView;
import com.kubee.pos.catalog.application.query.ItemView;
import com.kubee.pos.catalog.application.query.SearchItemsQuery;
import com.kubee.pos.catalog.application.query.VariantView;
import com.kubee.pos.catalog.domain.FoodType;
import com.kubee.pos.catalog.domain.ItemType;
import com.kubee.pos.common.tenant.TenantContext;
import com.kubee.pos.common.web.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class JdbcCatalogQueryRepository implements CatalogQueryRepository {

    private static final String CATEGORY_SELECT = """
            SELECT c.uuid, c.name, p.uuid AS parent_uuid, c.image_url, c.sort_order, c.is_active,
                   (SELECT count(*) FROM pos.items i WHERE i.category_id = c.id AND i.is_deleted = false) AS item_count
            FROM pos.categories c
            LEFT JOIN pos.categories p ON p.id = c.parent_id
            WHERE c.tenant_uuid = :tenant AND c.is_deleted = false
            """;

    private static final String ITEM_FROM = """
            FROM pos.items i
            LEFT JOIN pos.categories c ON c.id = i.category_id AND c.is_deleted = false
            LEFT JOIN pos.tax_groups tg ON tg.id = i.tax_group_id AND tg.is_deleted = false
            WHERE i.tenant_uuid = :tenant AND i.is_deleted = false
            """;

    private static final String ITEM_SELECT = """
            SELECT i.id, i.uuid, i.name, i.short_name, i.item_code, i.barcode,
                   c.uuid AS category_uuid, c.name AS category_name,
                   i.item_type, i.food_type, i.unit_of_measure, i.selling_price, i.mrp, i.price_includes_tax,
                   tg.uuid AS tax_group_uuid, tg.name AS tax_group_name, tg.total_rate AS tax_rate,
                   i.hsn_sac_code, i.has_variants, i.is_open_price, i.is_favourite, i.image_url, i.description,
                   i.sort_order, i.is_active, i.updated_at
            """ + ITEM_FROM;

    private static final String ITEM_ORDER = " ORDER BY i.sort_order, lower(i.name), i.id";

    private static final String ADDON_GROUP_SELECT = """
            SELECT g.id, g.uuid, g.name, g.min_select, g.max_select, g.is_active
            FROM pos.addon_groups g
            WHERE g.tenant_uuid = :tenant AND g.is_deleted = false
            """;

    private final NamedParameterJdbcTemplate jdbc;

    // ---------------------------------------------------------------- categories

    @Override
    public List<CategoryView> findCategories(Boolean active) {
        var params = tenantParams();
        String sql = CATEGORY_SELECT;
        if (active != null) {
            sql += " AND c.is_active = :active";
            params.addValue("active", active);
        }
        return jdbc.query(sql + " ORDER BY c.sort_order, lower(c.name)", params, CATEGORY_MAPPER);
    }

    @Override
    public Optional<CategoryView> findCategory(String categoryUuid) {
        return jdbc.query(CATEGORY_SELECT + " AND c.uuid = :uuid",
                tenantParams().addValue("uuid", categoryUuid), CATEGORY_MAPPER).stream().findFirst();
    }

    // ---------------------------------------------------------------- items

    @Override
    public PageResult<ItemView> searchItems(SearchItemsQuery q) {
        var params = tenantParams();
        StringBuilder where = new StringBuilder();
        if (q.search() != null && !q.search().isBlank()) {
            String term = q.search().trim();
            where.append("""
                     AND (i.name ILIKE :like ESCAPE '\\' OR i.item_code = :term OR i.barcode = :term
                          OR EXISTS (SELECT 1 FROM pos.item_variants v WHERE v.item_id = i.id AND v.is_deleted = false
                                     AND (v.item_code = :term OR v.barcode = :term)))
                    """);
            params.addValue("term", term).addValue("like", "%" + escapeLike(term) + "%");
        }
        if (q.categoryUuid() != null) {
            where.append(" AND c.uuid = :categoryUuid");
            params.addValue("categoryUuid", q.categoryUuid());
        }
        if (q.active() != null) {
            where.append(" AND i.is_active = :active");
            params.addValue("active", q.active());
        }
        if (q.favourite() != null) {
            where.append(" AND i.is_favourite = :favourite");
            params.addValue("favourite", q.favourite());
        }
        if (q.foodType() != null) {
            where.append(" AND i.food_type = :foodType");
            params.addValue("foodType", q.foodType().name());
        }

        Long total = jdbc.queryForObject("SELECT count(*) " + ITEM_FROM + where, params, Long.class);
        params.addValue("limit", q.size()).addValue("offset", (long) q.page() * q.size());
        List<ItemRow> rows = jdbc.query(ITEM_SELECT + where + ITEM_ORDER + " LIMIT :limit OFFSET :offset",
                params, ITEM_ROW_MAPPER);
        return PageResult.of(toViews(rows), q.page(), q.size(), total == null ? 0 : total);
    }

    @Override
    public Optional<ItemView> findItem(String itemUuid) {
        return findOneItem(" AND i.uuid = :uuid", tenantParams().addValue("uuid", itemUuid));
    }

    @Override
    public Optional<ItemLookupView> lookupByCode(String code) {
        var params = tenantParams().addValue("code", code.trim());
        Optional<ItemView> byItem = findOneItem(
                " AND i.is_active = true AND (i.item_code = :code OR i.barcode = :code)", params);
        if (byItem.isPresent()) {
            return Optional.of(new ItemLookupView(null, byItem.get()));
        }
        return jdbc.query("""
                        SELECT v.uuid AS variant_uuid, v.item_id
                        FROM pos.item_variants v
                        JOIN pos.items i ON i.id = v.item_id AND i.is_deleted = false AND i.is_active = true
                        WHERE v.tenant_uuid = :tenant AND v.is_deleted = false AND v.is_active = true
                          AND (v.item_code = :code OR v.barcode = :code)
                        LIMIT 1
                        """, params,
                        (rs, n) -> Map.entry(rs.getString("variant_uuid"), rs.getLong("item_id")))
                .stream().findFirst()
                .flatMap(match -> findOneItem(" AND i.id = :id", tenantParams().addValue("id", match.getValue()))
                        .map(item -> new ItemLookupView(match.getKey(), item)));
    }

    private Optional<ItemView> findOneItem(String condition, MapSqlParameterSource params) {
        List<ItemRow> rows = jdbc.query(ITEM_SELECT + condition + ITEM_ORDER + " LIMIT 1", params, ITEM_ROW_MAPPER);
        return toViews(rows).stream().findFirst();
    }

    /** Loads variants and add-on links for all rows in two queries (no N+1). */
    private List<ItemView> toViews(List<ItemRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> ids = rows.stream().map(ItemRow::id).toList();
        Map<Long, List<VariantView>> variants = loadVariants(ids);
        Map<Long, List<String>> addonGroups = loadAddonGroupUuids(ids);
        return rows.stream()
                .map(r -> r.toView(variants.getOrDefault(r.id(), List.of()), addonGroups.getOrDefault(r.id(), List.of())))
                .toList();
    }

    private Map<Long, List<VariantView>> loadVariants(Collection<Long> itemIds) {
        Map<Long, List<VariantView>> byItem = new LinkedHashMap<>();
        jdbc.query("""
                SELECT item_id, uuid, name, item_code, barcode, selling_price, mrp, is_default, sort_order, is_active
                FROM pos.item_variants
                WHERE tenant_uuid = :tenant AND is_deleted = false AND item_id IN (:ids)
                ORDER BY sort_order, id
                """, tenantParams().addValue("ids", itemIds), rs -> {
            byItem.computeIfAbsent(rs.getLong("item_id"), k -> new ArrayList<>()).add(new VariantView(
                    rs.getString("uuid"), rs.getString("name"), rs.getString("item_code"), rs.getString("barcode"),
                    rs.getBigDecimal("selling_price"), rs.getBigDecimal("mrp"), rs.getBoolean("is_default"),
                    rs.getInt("sort_order"), rs.getBoolean("is_active")));
        });
        return byItem;
    }

    private Map<Long, List<String>> loadAddonGroupUuids(Collection<Long> itemIds) {
        Map<Long, List<String>> byItem = new LinkedHashMap<>();
        jdbc.query("""
                SELECT l.item_id, g.uuid
                FROM pos.item_addon_groups l
                JOIN pos.addon_groups g ON g.id = l.addon_group_id AND g.is_deleted = false
                WHERE l.tenant_uuid = :tenant AND l.is_deleted = false AND l.item_id IN (:ids)
                ORDER BY l.sort_order, l.id
                """, tenantParams().addValue("ids", itemIds), rs -> {
            byItem.computeIfAbsent(rs.getLong("item_id"), k -> new ArrayList<>()).add(rs.getString("uuid"));
        });
        return byItem;
    }

    // ---------------------------------------------------------------- add-on groups

    @Override
    public List<AddonGroupView> findAddonGroups(Boolean active) {
        var params = tenantParams();
        String sql = ADDON_GROUP_SELECT;
        if (active != null) {
            sql += " AND g.is_active = :active";
            params.addValue("active", active);
        }
        return withAddons(jdbc.query(sql + " ORDER BY lower(g.name)", params, ADDON_GROUP_ROW_MAPPER));
    }

    @Override
    public Optional<AddonGroupView> findAddonGroup(String addonGroupUuid) {
        return withAddons(jdbc.query(ADDON_GROUP_SELECT + " AND g.uuid = :uuid",
                tenantParams().addValue("uuid", addonGroupUuid), ADDON_GROUP_ROW_MAPPER)).stream().findFirst();
    }

    private List<AddonGroupView> withAddons(List<AddonGroupRow> groups) {
        if (groups.isEmpty()) {
            return List.of();
        }
        Map<Long, List<AddonView>> byGroup = new LinkedHashMap<>();
        jdbc.query("""
                SELECT addon_group_id, uuid, name, price, food_type, sort_order, is_active
                FROM pos.addons
                WHERE tenant_uuid = :tenant AND is_deleted = false AND addon_group_id IN (:ids)
                ORDER BY sort_order, id
                """, tenantParams().addValue("ids", groups.stream().map(AddonGroupRow::id).toList()), rs -> {
            byGroup.computeIfAbsent(rs.getLong("addon_group_id"), k -> new ArrayList<>()).add(new AddonView(
                    rs.getString("uuid"), rs.getString("name"), rs.getBigDecimal("price"),
                    enumOrNull(FoodType.class, rs.getString("food_type")), rs.getInt("sort_order"),
                    rs.getBoolean("is_active")));
        });
        return groups.stream()
                .map(g -> new AddonGroupView(g.uuid(), g.name(), g.minSelect(), g.maxSelect(), g.active(),
                        byGroup.getOrDefault(g.id(), List.of())))
                .toList();
    }

    // ---------------------------------------------------------------- mapping

    private record ItemRow(long id, ItemView partial) {
        ItemView toView(List<VariantView> variants, List<String> addonGroupUuids) {
            ItemView p = partial;
            return new ItemView(p.uuid(), p.name(), p.shortName(), p.itemCode(), p.barcode(), p.categoryUuid(),
                    p.categoryName(), p.itemType(), p.foodType(), p.unitOfMeasure(), p.sellingPrice(), p.mrp(),
                    p.priceIncludesTax(), p.taxGroupUuid(), p.taxGroupName(), p.taxRate(), p.hsnSacCode(),
                    p.hasVariants(), p.openPrice(), p.favourite(), p.imageUrl(), p.description(), p.sortOrder(),
                    p.active(), p.updatedAt(), variants, addonGroupUuids);
        }
    }

    private record AddonGroupRow(long id, String uuid, String name, int minSelect, Integer maxSelect, boolean active) {
    }

    private static final RowMapper<CategoryView> CATEGORY_MAPPER = (rs, n) -> new CategoryView(
            rs.getString("uuid"), rs.getString("name"), rs.getString("parent_uuid"), rs.getString("image_url"),
            rs.getInt("sort_order"), rs.getBoolean("is_active"), rs.getLong("item_count"));

    private static final RowMapper<ItemRow> ITEM_ROW_MAPPER = (rs, n) -> new ItemRow(rs.getLong("id"), new ItemView(
            rs.getString("uuid"), rs.getString("name"), rs.getString("short_name"), rs.getString("item_code"),
            rs.getString("barcode"), rs.getString("category_uuid"), rs.getString("category_name"),
            enumOrNull(ItemType.class, rs.getString("item_type")), enumOrNull(FoodType.class, rs.getString("food_type")),
            rs.getString("unit_of_measure"), rs.getBigDecimal("selling_price"), rs.getBigDecimal("mrp"),
            rs.getBoolean("price_includes_tax"), rs.getString("tax_group_uuid"), rs.getString("tax_group_name"),
            rs.getBigDecimal("tax_rate"), rs.getString("hsn_sac_code"), rs.getBoolean("has_variants"),
            rs.getBoolean("is_open_price"), rs.getBoolean("is_favourite"), rs.getString("image_url"),
            rs.getString("description"), rs.getInt("sort_order"), rs.getBoolean("is_active"),
            toLocalDateTime(rs.getTimestamp("updated_at")), null, null));

    private static final RowMapper<AddonGroupRow> ADDON_GROUP_ROW_MAPPER = (rs, n) -> new AddonGroupRow(
            rs.getLong("id"), rs.getString("uuid"), rs.getString("name"), rs.getInt("min_select"),
            nullableInt(rs, "max_select"), rs.getBoolean("is_active"));

    private static MapSqlParameterSource tenantParams() {
        return new MapSqlParameterSource("tenant", TenantContext.requireTenantUuid());
    }

    private static String escapeLike(String term) {
        return term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static <E extends Enum<E>> E enumOrNull(Class<E> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
