package com.kubee.pos.ordering.infrastructure.persistence;

import com.kubee.pos.common.tenant.TenantContext;
import com.kubee.pos.ordering.application.port.IssuedBillPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Reads pos.bills directly, so ordering never imports billing classes. */
@Component
@RequiredArgsConstructor
class JdbcIssuedBillPort implements IssuedBillPort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Optional<String> issuedBillNumber(Long orderId) {
        var params = new MapSqlParameterSource("tenant", TenantContext.requireTenantUuid()).addValue("orderId", orderId);
        return jdbc.queryForList("""
                SELECT bill_number FROM pos.bills
                WHERE tenant_uuid = :tenant AND order_id = :orderId AND status = 'ISSUED' AND is_deleted = false
                """, params, String.class).stream().findFirst();
    }
}
