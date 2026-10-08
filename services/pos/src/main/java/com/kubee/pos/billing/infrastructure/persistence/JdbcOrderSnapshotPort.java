package com.kubee.pos.billing.infrastructure.persistence;

import com.kubee.pos.billing.application.port.OrderSnapshotPort;
import com.kubee.pos.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads the ordering tables directly (one database). Keeps billing independent of ordering classes;
 * if ordering moves out, only this adapter changes.
 */
@Component
@RequiredArgsConstructor
class JdbcOrderSnapshotPort implements OrderSnapshotPort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Optional<OrderSnapshot> findOrder(String orderUuid) {
        var params = new MapSqlParameterSource("tenant", TenantContext.requireTenantUuid()).addValue("uuid", orderUuid);
        List<OrderSnapshot> orders = jdbc.query("""
                SELECT id, uuid, order_number, status, payment_status, customer_name, customer_phone, sub_total, discount_amount,
                       taxable_amount, tax_amount, round_off_amount, grand_total
                FROM pos.orders WHERE tenant_uuid = :tenant AND uuid = :uuid AND is_deleted = false
                """, params, (rs, n) -> new OrderSnapshot(rs.getLong("id"), rs.getString("uuid"),
                rs.getString("order_number"), rs.getString("status"), rs.getString("payment_status"),
                rs.getString("customer_name"),
                rs.getString("customer_phone"), rs.getBigDecimal("sub_total"), rs.getBigDecimal("discount_amount"),
                rs.getBigDecimal("taxable_amount"), rs.getBigDecimal("tax_amount"),
                rs.getBigDecimal("round_off_amount"), rs.getBigDecimal("grand_total"), null));
        if (orders.isEmpty()) {
            return Optional.empty();
        }
        OrderSnapshot o = orders.getFirst();
        params.addValue("orderId", o.id());
        return Optional.of(new OrderSnapshot(o.id(), o.uuid(), o.orderNumber(), o.status(), o.paymentStatus(),
                o.customerName(),
                o.customerPhone(), o.subTotal(), o.discountAmount(), o.taxableAmount(), o.taxAmount(),
                o.roundOffAmount(), o.grandTotal(), lines(params)));
    }

    private List<LineSnapshot> lines(MapSqlParameterSource params) {
        Map<Long, List<String>> addons = new HashMap<>();
        jdbc.query("""
                SELECT a.order_item_id, a.addon_name, a.quantity
                FROM pos.order_item_addons a
                JOIN pos.order_items l ON l.id = a.order_item_id
                WHERE l.order_id = :orderId AND a.tenant_uuid = :tenant AND a.is_deleted = false
                ORDER BY a.id
                """, params, rs -> {
            BigDecimal qty = rs.getBigDecimal("quantity").stripTrailingZeros();
            String text = qty.compareTo(BigDecimal.ONE) == 0
                    ? rs.getString("addon_name")
                    : qty.toPlainString() + " x " + rs.getString("addon_name");
            addons.computeIfAbsent(rs.getLong("order_item_id"), k -> new ArrayList<>()).add(text);
        });

        return jdbc.query("""
                SELECT id, item_id, item_name, variant_name, hsn_sac_code, unit_of_measure, quantity,
                       unit_price + addons_unit_price AS unit_price, line_amount, discount_amount, tax_group_id,
                       tax_rate, taxable_amount, tax_amount, total_amount
                FROM pos.order_items
                WHERE order_id = :orderId AND tenant_uuid = :tenant AND is_deleted = false
                ORDER BY sort_order, id
                """, params, (rs, n) -> {
            long id = rs.getLong("id");
            List<String> names = addons.get(id);
            return new LineSnapshot(id, nullableLong(rs, "item_id"), rs.getString("item_name"),
                    rs.getString("variant_name"), names == null ? null : "+ " + String.join(", ", names),
                    rs.getString("hsn_sac_code"), rs.getString("unit_of_measure"), rs.getBigDecimal("quantity"),
                    rs.getBigDecimal("unit_price"), rs.getBigDecimal("line_amount"),
                    rs.getBigDecimal("discount_amount"), nullableLong(rs, "tax_group_id"), rs.getBigDecimal("tax_rate"),
                    rs.getBigDecimal("taxable_amount"), rs.getBigDecimal("tax_amount"),
                    rs.getBigDecimal("total_amount"));
        });
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
