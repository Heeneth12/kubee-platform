package com.kubee.pos.billing.infrastructure.persistence;

import com.kubee.pos.billing.application.port.SellerPort;
import com.kubee.pos.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Reads pos.pos_settings directly until the shop/settings module exists. */
@Component
@RequiredArgsConstructor
class JdbcSellerPort implements SellerPort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Optional<SellerSettings> currentSeller() {
        var params = new MapSqlParameterSource("tenant", TenantContext.requireTenantUuid());
        return jdbc.query("""
                SELECT shop_name, address, gstin, state_code, bill_prefix, financial_year_start_month
                FROM pos.pos_settings WHERE tenant_uuid = :tenant AND is_deleted = false
                """, params, (rs, n) -> new SellerSettings(rs.getString("shop_name"), rs.getString("address"),
                rs.getString("gstin"), rs.getString("state_code"), rs.getString("bill_prefix"),
                rs.getInt("financial_year_start_month"))).stream().findFirst();
    }
}
