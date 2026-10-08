package com.kubee.pos.catalog.infrastructure.persistence;

import com.kubee.pos.catalog.domain.CatalogConstraints;
import com.kubee.pos.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class JdbcCatalogConstraints implements CatalogConstraints {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public boolean isItemCodeTaken(String itemCode, Long excludeItemId) {
        return codeTaken("item_code", itemCode, excludeItemId);
    }

    @Override
    public boolean isBarcodeTaken(String barcode, Long excludeItemId) {
        return codeTaken("barcode", barcode, excludeItemId);
    }

    @Override
    public boolean isCategoryNameTaken(String name, Long parentId, Long excludeCategoryId) {
        return exists("""
                SELECT 1 FROM pos.categories
                WHERE tenant_uuid = :tenant AND is_deleted = false
                  AND lower(name) = lower(:name)
                  AND COALESCE(parent_id, 0) = COALESCE(CAST(:parentId AS BIGINT), 0)
                  AND (CAST(:excludeId AS BIGINT) IS NULL OR id <> :excludeId)
                """, params().addValue("name", name).addValue("parentId", parentId).addValue("excludeId", excludeCategoryId));
    }

    @Override
    public boolean isAddonGroupNameTaken(String name, Long excludeAddonGroupId) {
        return exists("""
                SELECT 1 FROM pos.addon_groups
                WHERE tenant_uuid = :tenant AND is_deleted = false
                  AND lower(name) = lower(:name)
                  AND (CAST(:excludeId AS BIGINT) IS NULL OR id <> :excludeId)
                """, params().addValue("name", name).addValue("excludeId", excludeAddonGroupId));
    }

    @Override
    public boolean categoryHasItems(Long categoryId) {
        return exists("""
                SELECT 1 FROM pos.items
                WHERE tenant_uuid = :tenant AND is_deleted = false AND category_id = :id
                """, params().addValue("id", categoryId));
    }

    @Override
    public boolean categoryHasChildren(Long categoryId) {
        return exists("""
                SELECT 1 FROM pos.categories
                WHERE tenant_uuid = :tenant AND is_deleted = false AND parent_id = :id
                """, params().addValue("id", categoryId));
    }

    @Override
    public boolean isAddonGroupInUse(Long addonGroupId) {
        return exists("""
                SELECT 1 FROM pos.item_addon_groups l
                JOIN pos.items i ON i.id = l.item_id AND i.is_deleted = false
                WHERE l.tenant_uuid = :tenant AND l.is_deleted = false AND l.addon_group_id = :id
                """, params().addValue("id", addonGroupId));
    }

    /** Codes and barcodes must be unique across items AND variants, so a scan always finds one thing. */
    private boolean codeTaken(String column, String value, Long excludeItemId) {
        return exists("""
                SELECT 1 FROM pos.items
                WHERE tenant_uuid = :tenant AND is_deleted = false AND %1$s = :value
                  AND (CAST(:excludeId AS BIGINT) IS NULL OR id <> :excludeId)
                UNION ALL
                SELECT 1 FROM pos.item_variants v
                JOIN pos.items i ON i.id = v.item_id AND i.is_deleted = false
                WHERE v.tenant_uuid = :tenant AND v.is_deleted = false AND v.%1$s = :value
                  AND (CAST(:excludeId AS BIGINT) IS NULL OR v.item_id <> :excludeId)
                """.formatted(column), params().addValue("value", value).addValue("excludeId", excludeItemId));
    }

    private boolean exists(String sql, MapSqlParameterSource params) {
        Boolean found = jdbc.queryForObject("SELECT EXISTS (" + sql + ")", params, Boolean.class);
        return Boolean.TRUE.equals(found);
    }

    private static MapSqlParameterSource params() {
        return new MapSqlParameterSource("tenant", TenantContext.requireTenantUuid());
    }
}
