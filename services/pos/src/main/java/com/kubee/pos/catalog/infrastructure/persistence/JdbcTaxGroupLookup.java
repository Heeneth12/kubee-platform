package com.kubee.pos.catalog.infrastructure.persistence;

import com.kubee.pos.catalog.application.port.TaxGroupLookup;
import com.kubee.pos.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Reads pos.tax_groups directly until the tax module exists; then this adapter
 * should call the tax module's query side instead.
 */
@Component
@RequiredArgsConstructor
class JdbcTaxGroupLookup implements TaxGroupLookup {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Optional<Long> findIdByUuid(String taxGroupUuid) {
        var params = new MapSqlParameterSource("tenant", TenantContext.requireTenantUuid())
                .addValue("uuid", taxGroupUuid);
        return jdbc.queryForList("""
                SELECT id FROM pos.tax_groups
                WHERE tenant_uuid = :tenant AND uuid = :uuid AND is_deleted = false
                """, params, Long.class).stream().findFirst();
    }
}
