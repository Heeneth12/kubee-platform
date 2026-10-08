package com.kubee.pos.billing.infrastructure.persistence;

import com.kubee.pos.billing.application.port.TaxComponentPort;
import com.kubee.pos.billing.domain.TaxComponentSpec;
import com.kubee.pos.billing.domain.TaxType;
import com.kubee.pos.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Reads pos.tax_components directly until the tax module exists. */
@Component
@RequiredArgsConstructor
class JdbcTaxComponentPort implements TaxComponentPort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Map<Long, List<TaxComponentSpec>> componentsOf(Collection<Long> taxGroupIds) {
        Map<Long, List<TaxComponentSpec>> result = new HashMap<>();
        if (taxGroupIds.isEmpty()) {
            return result;
        }
        var params = new MapSqlParameterSource("tenant", TenantContext.requireTenantUuid())
                .addValue("ids", taxGroupIds);
        jdbc.query("""
                SELECT tax_group_id, id, tax_type, name, rate FROM pos.tax_components
                WHERE tenant_uuid = :tenant AND tax_group_id IN (:ids) AND is_deleted = false
                ORDER BY tax_group_id, sort_order, id
                """, params, rs -> {
            result.computeIfAbsent(rs.getLong("tax_group_id"), k -> new ArrayList<>()).add(new TaxComponentSpec(
                    rs.getLong("id"), TaxType.valueOf(rs.getString("tax_type")), rs.getString("name"),
                    rs.getBigDecimal("rate")));
        });
        return result;
    }
}
