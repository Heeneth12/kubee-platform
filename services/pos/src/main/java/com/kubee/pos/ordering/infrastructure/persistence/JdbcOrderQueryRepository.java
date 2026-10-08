package com.kubee.pos.ordering.infrastructure.persistence;

import com.kubee.pos.common.tenant.TenantContext;
import com.kubee.pos.common.web.PageResult;
import com.kubee.pos.ordering.application.query.OrderLineAddonView;
import com.kubee.pos.ordering.application.query.OrderLineView;
import com.kubee.pos.ordering.application.query.OrderQueryRepository;
import com.kubee.pos.ordering.application.query.OrderSummaryView;
import com.kubee.pos.ordering.application.query.OrderView;
import com.kubee.pos.ordering.application.query.PaymentView;
import com.kubee.pos.ordering.application.query.SearchOrdersQuery;
import com.kubee.pos.ordering.domain.DiscountType;
import com.kubee.pos.ordering.domain.OrderSource;
import com.kubee.pos.ordering.domain.OrderStatus;
import com.kubee.pos.ordering.domain.OrderType;
import com.kubee.pos.ordering.domain.PaymentMethod;
import com.kubee.pos.ordering.domain.PaymentStatus;
import com.kubee.pos.ordering.domain.PaymentType;
import com.kubee.pos.ordering.domain.TransactionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class JdbcOrderQueryRepository implements OrderQueryRepository {

    private static final String ORDER_FROM = """
            FROM pos.orders o
            WHERE o.tenant_uuid = :tenant AND o.is_deleted = false
            """;

    private static final String SUMMARY_SELECT = """
            SELECT o.uuid, o.order_number, o.order_type, o.source, o.external_order_id, o.status, o.payment_status, o.customer_name,
                   o.customer_phone, o.table_label, o.grand_total, o.paid_amount, o.created_at, o.completed_at,
                   (SELECT count(*) FROM pos.order_items l WHERE l.order_id = o.id AND l.is_deleted = false) AS line_count
            """ + ORDER_FROM;

    private final NamedParameterJdbcTemplate jdbc;

    // ---------------------------------------------------------------- one order

    @Override
    public Optional<OrderView> findOrder(String orderUuid) {
        var params = tenantParams().addValue("uuid", orderUuid);
        List<OrderRow> rows = jdbc.query("SELECT o.* " + ORDER_FROM + " AND o.uuid = :uuid", params, ORDER_ROW_MAPPER);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        OrderRow o = rows.getFirst();
        params.addValue("orderId", o.id());
        return Optional.of(new OrderView(o.uuid(), o.orderNumber(), o.clientRef(), o.orderType(), o.source(),
                o.externalOrderId(), o.status(),
                o.paymentStatus(), o.customerName(), o.customerPhone(), o.tableLabel(), o.notes(), o.subTotal(),
                o.discountType(), o.discountValue(), o.discountAmount(), o.discountReason(), o.taxableAmount(),
                o.taxAmount(), o.roundOffAmount(), o.grandTotal(), o.paidAmount(), due(o.status(), o.grandTotal(), o.paidAmount()),
                o.createdBy(), o.createdAt(), o.updatedAt(), o.completedAt(), o.cancelledAt(), o.cancelledBy(),
                o.cancelReason(), lines(params), payments(params)));
    }

    private List<OrderLineView> lines(MapSqlParameterSource params) {
        Map<Long, List<OrderLineAddonView>> addons = new LinkedHashMap<>();
        jdbc.query("""
                SELECT a.order_item_id, a.uuid, ad.uuid AS addon_uuid, a.addon_name, a.quantity, a.unit_price
                FROM pos.order_item_addons a
                JOIN pos.order_items l ON l.id = a.order_item_id
                LEFT JOIN pos.addons ad ON ad.id = a.addon_id AND ad.is_deleted = false
                WHERE l.order_id = :orderId AND a.tenant_uuid = :tenant AND a.is_deleted = false
                ORDER BY a.id
                """, params, rs -> {
            addons.computeIfAbsent(rs.getLong("order_item_id"), k -> new ArrayList<>()).add(new OrderLineAddonView(
                    rs.getString("uuid"), rs.getString("addon_uuid"), rs.getString("addon_name"),
                    rs.getBigDecimal("quantity"), rs.getBigDecimal("unit_price")));
        });

        return jdbc.query("""
                SELECT l.*, i.uuid AS item_uuid, v.uuid AS variant_uuid
                FROM pos.order_items l
                LEFT JOIN pos.items i ON i.id = l.item_id AND i.is_deleted = false
                LEFT JOIN pos.item_variants v ON v.id = l.item_variant_id AND v.is_deleted = false
                WHERE l.order_id = :orderId AND l.tenant_uuid = :tenant AND l.is_deleted = false
                ORDER BY l.sort_order, l.id
                """, params, (rs, n) -> new OrderLineView(rs.getString("uuid"), rs.getString("item_uuid"),
                rs.getString("variant_uuid"), rs.getString("item_name"), rs.getString("variant_name"),
                rs.getString("hsn_sac_code"), rs.getString("unit_of_measure"), rs.getBigDecimal("quantity"),
                rs.getBigDecimal("unit_price"), rs.getBigDecimal("addons_unit_price"),
                rs.getBoolean("price_includes_tax"), rs.getBigDecimal("line_amount"),
                enumOrNull(DiscountType.class, rs.getString("discount_type")), rs.getBigDecimal("discount_value"),
                rs.getBigDecimal("discount_amount"), rs.getBigDecimal("tax_rate"), rs.getBigDecimal("taxable_amount"),
                rs.getBigDecimal("tax_amount"), rs.getBigDecimal("total_amount"), rs.getString("notes"),
                List.copyOf(addons.getOrDefault(rs.getLong("id"), List.of()))));
    }

    private List<PaymentView> payments(MapSqlParameterSource params) {
        return jdbc.query("""
                SELECT p.*, orig.uuid AS refund_of_uuid,
                       CASE WHEN p.payment_type = 'PAYMENT' AND p.status = 'SUCCESS' THEN
                           p.amount - COALESCE((SELECT sum(r.amount) FROM pos.payments r
                                                WHERE r.refund_of_payment_id = p.id AND r.status = 'SUCCESS'
                                                  AND r.is_deleted = false), 0)
                       END AS refundable_amount
                FROM pos.payments p
                LEFT JOIN pos.payments orig ON orig.id = p.refund_of_payment_id
                WHERE p.order_id = :orderId AND p.tenant_uuid = :tenant AND p.is_deleted = false
                ORDER BY p.paid_at, p.id
                """, params, (rs, n) -> new PaymentView(rs.getString("uuid"), rs.getString("client_ref"),
                PaymentType.valueOf(rs.getString("payment_type")), PaymentMethod.valueOf(rs.getString("payment_method")),
                TransactionStatus.valueOf(rs.getString("status")), rs.getBigDecimal("amount"),
                rs.getBigDecimal("tendered_amount"), rs.getBigDecimal("change_amount"),
                rs.getBigDecimal("refundable_amount"), rs.getString("reference_no"), rs.getString("refund_of_uuid"),
                rs.getString("notes"), toLocalDateTime(rs.getTimestamp("paid_at")), rs.getString("received_by")));
    }

    // ---------------------------------------------------------------- search

    @Override
    public PageResult<OrderSummaryView> searchOrders(SearchOrdersQuery q) {
        var params = tenantParams();
        StringBuilder where = new StringBuilder();
        if (q.status() != null) {
            where.append(" AND o.status = :status");
            params.addValue("status", q.status().name());
        }
        if (q.paymentStatus() != null) {
            where.append(" AND o.payment_status = :paymentStatus");
            params.addValue("paymentStatus", q.paymentStatus().name());
        }
        if (q.orderType() != null) {
            where.append(" AND o.order_type = :orderType");
            params.addValue("orderType", q.orderType().name());
        }
        if (q.source() != null) {
            where.append(" AND o.source = :source");
            params.addValue("source", q.source().name());
        }
        if (q.from() != null) {
            where.append(" AND o.created_at >= :from");
            params.addValue("from", q.from().atStartOfDay());
        }
        if (q.to() != null) {
            where.append(" AND o.created_at < :to");
            params.addValue("to", q.to().plusDays(1).atStartOfDay());
        }
        if (q.search() != null && !q.search().isBlank()) {
            String term = q.search().trim();
            where.append("""
                     AND (o.order_number = :term OR o.external_order_id = :term
                          OR o.customer_phone LIKE :like ESCAPE '\\'
                          OR o.customer_name ILIKE :like ESCAPE '\\')
                    """);
            params.addValue("term", term).addValue("like", "%" + escapeLike(term) + "%");
        }

        Long total = jdbc.queryForObject("SELECT count(*) " + ORDER_FROM + where, params, Long.class);
        params.addValue("limit", q.size()).addValue("offset", (long) q.page() * q.size());
        List<OrderSummaryView> rows = jdbc.query(
                SUMMARY_SELECT + where + " ORDER BY o.created_at DESC, o.id DESC LIMIT :limit OFFSET :offset",
                params, SUMMARY_MAPPER);
        return PageResult.of(rows, q.page(), q.size(), total == null ? 0 : total);
    }

    // ---------------------------------------------------------------- mapping

    private record OrderRow(long id, String uuid, String orderNumber, String clientRef, OrderType orderType,
                            OrderSource source, String externalOrderId, OrderStatus status, PaymentStatus paymentStatus, String customerName, String customerPhone,
                            String tableLabel, String notes, BigDecimal subTotal, DiscountType discountType,
                            BigDecimal discountValue, BigDecimal discountAmount, String discountReason,
                            BigDecimal taxableAmount, BigDecimal taxAmount, BigDecimal roundOffAmount,
                            BigDecimal grandTotal, BigDecimal paidAmount, String createdBy, LocalDateTime createdAt,
                            LocalDateTime updatedAt, LocalDateTime completedAt, LocalDateTime cancelledAt,
                            String cancelledBy, String cancelReason) {
    }

    private static final RowMapper<OrderRow> ORDER_ROW_MAPPER = (rs, n) -> new OrderRow(rs.getLong("id"),
            rs.getString("uuid"), rs.getString("order_number"), rs.getString("client_ref"),
            OrderType.valueOf(rs.getString("order_type")), OrderSource.valueOf(rs.getString("source")),
            rs.getString("external_order_id"), OrderStatus.valueOf(rs.getString("status")),
            PaymentStatus.valueOf(rs.getString("payment_status")), rs.getString("customer_name"),
            rs.getString("customer_phone"), rs.getString("table_label"), rs.getString("notes"),
            rs.getBigDecimal("sub_total"), enumOrNull(DiscountType.class, rs.getString("discount_type")),
            rs.getBigDecimal("discount_value"), rs.getBigDecimal("discount_amount"), rs.getString("discount_reason"),
            rs.getBigDecimal("taxable_amount"), rs.getBigDecimal("tax_amount"), rs.getBigDecimal("round_off_amount"),
            rs.getBigDecimal("grand_total"), rs.getBigDecimal("paid_amount"), rs.getString("created_by"),
            toLocalDateTime(rs.getTimestamp("created_at")), toLocalDateTime(rs.getTimestamp("updated_at")),
            toLocalDateTime(rs.getTimestamp("completed_at")), toLocalDateTime(rs.getTimestamp("cancelled_at")),
            rs.getString("cancelled_by"), rs.getString("cancel_reason"));

    private static final RowMapper<OrderSummaryView> SUMMARY_MAPPER = (rs, n) -> new OrderSummaryView(
            rs.getString("uuid"), rs.getString("order_number"), OrderType.valueOf(rs.getString("order_type")),
            OrderSource.valueOf(rs.getString("source")), rs.getString("external_order_id"),
            OrderStatus.valueOf(rs.getString("status")), PaymentStatus.valueOf(rs.getString("payment_status")),
            rs.getString("customer_name"), rs.getString("customer_phone"), rs.getString("table_label"),
            rs.getInt("line_count"), rs.getBigDecimal("grand_total"), rs.getBigDecimal("paid_amount"),
            due(OrderStatus.valueOf(rs.getString("status")), rs.getBigDecimal("grand_total"),
                    rs.getBigDecimal("paid_amount")),
            toLocalDateTime(rs.getTimestamp("created_at")), toLocalDateTime(rs.getTimestamp("completed_at")));

    /** Same rule as {@code Order.dueAmount()}: nothing is due on a completed or cancelled order. */
    private static BigDecimal due(OrderStatus status, BigDecimal grandTotal, BigDecimal paid) {
        if (status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) {
            return BigDecimal.ZERO.setScale(2);
        }
        return grandTotal.subtract(paid).max(BigDecimal.ZERO.setScale(2));
    }

    private static MapSqlParameterSource tenantParams() {
        return new MapSqlParameterSource("tenant", TenantContext.requireTenantUuid());
    }

    private static String escapeLike(String term) {
        return term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static <E extends Enum<E>> E enumOrNull(Class<E> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
