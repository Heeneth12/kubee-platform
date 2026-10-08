package com.kubee.pos.shift.infrastructure.persistence;

import com.kubee.pos.common.domain.Money;
import com.kubee.pos.common.tenant.TenantContext;
import com.kubee.pos.shift.application.port.CashSalesPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Reads pos.payments directly, so the shift module never imports ordering classes. */
@Component
@RequiredArgsConstructor
class JdbcCashSalesPort implements CashSalesPort {

    private static final String NET_CASH_SQL = """
            SELECT coalesce(sum(CASE WHEN payment_type = 'PAYMENT' THEN amount ELSE -amount END), 0)
            FROM pos.payments
            WHERE tenant_uuid = :tenant AND is_deleted = false AND status = 'SUCCESS' AND payment_method = 'CASH'
              AND paid_at >= :from AND paid_at < :to
            """;

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Money netCash(LocalDateTime from, LocalDateTime to) {
        var params = new MapSqlParameterSource("tenant", TenantContext.requireTenantUuid())
                .addValue("from", from).addValue("to", to);
        BigDecimal net = jdbc.queryForObject(NET_CASH_SQL, params, BigDecimal.class);
        return Money.of(net == null ? BigDecimal.ZERO : net);
    }
}
