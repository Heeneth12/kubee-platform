package com.kubee.pos.shift.infrastructure.persistence;

import com.kubee.pos.common.tenant.TenantContext;
import com.kubee.pos.shift.application.query.ShiftQueryRepository;
import com.kubee.pos.shift.application.query.ShiftView;
import com.kubee.pos.shift.domain.CashMovementType;
import com.kubee.pos.shift.domain.ShiftStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class JdbcShiftQueryRepository implements ShiftQueryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Optional<ShiftView> findShift(String shiftUuid) {
        return find(" AND uuid = :uuid", tenantParams().addValue("uuid", shiftUuid));
    }

    @Override
    public Optional<ShiftView> findOpenShift() {
        return find(" AND status = 'OPEN'", tenantParams());
    }

    private Optional<ShiftView> find(String where, MapSqlParameterSource params) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM pos.shifts WHERE tenant_uuid = :tenant AND is_deleted = false" + where, params);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Map<String, Object> s = rows.getFirst();
        ShiftStatus status = ShiftStatus.valueOf((String) s.get("status"));
        LocalDateTime openedAt = toLocalDateTime(s.get("opened_at"));
        LocalDateTime windowEnd = status == ShiftStatus.OPEN ? LocalDateTime.now() : toLocalDateTime(s.get("closed_at"));
        params.addValue("shiftId", s.get("id")).addValue("from", openedAt).addValue("to", windowEnd);

        List<ShiftView.Movement> movements = jdbc.query("""
                SELECT uuid, movement_type, amount, reason, created_by, moved_at FROM pos.cash_movements
                WHERE shift_id = :shiftId AND tenant_uuid = :tenant AND is_deleted = false ORDER BY moved_at, id
                """, params, (rs, n) -> new ShiftView.Movement(rs.getString("uuid"),
                CashMovementType.valueOf(rs.getString("movement_type")), rs.getBigDecimal("amount"),
                rs.getString("reason"), rs.getString("created_by"), toLocalDateTime(rs.getTimestamp("moved_at"))));

        List<ShiftView.PaymentTotal> payments = jdbc.query("""
                SELECT payment_method,
                       count(*) FILTER (WHERE payment_type = 'PAYMENT') AS payments,
                       coalesce(sum(amount) FILTER (WHERE payment_type = 'PAYMENT'), 0) AS paid,
                       count(*) FILTER (WHERE payment_type = 'REFUND') AS refunds,
                       coalesce(sum(amount) FILTER (WHERE payment_type = 'REFUND'), 0) AS refunded
                FROM pos.payments
                WHERE tenant_uuid = :tenant AND is_deleted = false AND status = 'SUCCESS'
                  AND paid_at >= :from AND paid_at < :to
                GROUP BY payment_method ORDER BY paid DESC
                """, params, (rs, n) -> new ShiftView.PaymentTotal(rs.getString("payment_method"),
                rs.getLong("payments"), rs.getBigDecimal("paid"), rs.getLong("refunds"), rs.getBigDecimal("refunded"),
                rs.getBigDecimal("paid").subtract(rs.getBigDecimal("refunded"))));

        Map<String, Object> sales = jdbc.queryForMap("""
                SELECT count(*) AS orders, coalesce(sum(grand_total), 0) AS net FROM pos.orders
                WHERE tenant_uuid = :tenant AND is_deleted = false AND status = 'COMPLETED'
                  AND completed_at >= :from AND completed_at < :to
                """, params);

        BigDecimal opening = (BigDecimal) s.get("opening_cash");
        BigDecimal cashIn = sumMovements(movements, CashMovementType.IN);
        BigDecimal cashOut = sumMovements(movements, CashMovementType.OUT);
        BigDecimal cashSales = status == ShiftStatus.CLOSED ? (BigDecimal) s.get("cash_sales")
                : payments.stream().filter(p -> "CASH".equals(p.method())).map(ShiftView.PaymentTotal::netAmount)
                        .findFirst().orElse(BigDecimal.ZERO.setScale(2));
        BigDecimal expected = status == ShiftStatus.CLOSED ? (BigDecimal) s.get("expected_cash")
                : opening.add(cashSales).add(cashIn).subtract(cashOut);

        return Optional.of(new ShiftView((String) s.get("uuid"), status, (String) s.get("opened_by"), openedAt,
                opening, (String) s.get("opening_notes"), (String) s.get("closed_by"),
                toLocalDateTime(s.get("closed_at")), (String) s.get("closing_notes"), cashSales, cashIn, cashOut,
                expected, (BigDecimal) s.get("counted_cash"), (BigDecimal) s.get("cash_difference"),
                ((Number) sales.get("orders")).longValue(), (BigDecimal) sales.get("net"), payments, movements));
    }

    private static BigDecimal sumMovements(List<ShiftView.Movement> movements, CashMovementType type) {
        return movements.stream().filter(m -> m.type() == type).map(ShiftView.Movement::amount)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    private static MapSqlParameterSource tenantParams() {
        return new MapSqlParameterSource("tenant", TenantContext.requireTenantUuid());
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        return value == null ? null : ((Timestamp) value).toLocalDateTime();
    }
}
