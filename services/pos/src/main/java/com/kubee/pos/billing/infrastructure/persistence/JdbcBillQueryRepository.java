package com.kubee.pos.billing.infrastructure.persistence;

import com.kubee.pos.billing.application.query.BillLineTaxView;
import com.kubee.pos.billing.application.query.BillLineView;
import com.kubee.pos.billing.application.query.BillQueryRepository;
import com.kubee.pos.billing.application.query.BillSummaryView;
import com.kubee.pos.billing.application.query.BillView;
import com.kubee.pos.billing.application.query.HsnSummaryView;
import com.kubee.pos.billing.application.query.SearchBillsQuery;
import com.kubee.pos.billing.application.query.TaxSummaryView;
import com.kubee.pos.billing.domain.AmountInWords;
import com.kubee.pos.billing.domain.BillStatus;
import com.kubee.pos.billing.domain.ShareChannel;
import com.kubee.pos.billing.domain.TaxType;
import com.kubee.pos.common.domain.Money;
import com.kubee.pos.common.tenant.TenantContext;
import com.kubee.pos.common.web.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class JdbcBillQueryRepository implements BillQueryRepository {

    private static final String BILL_FROM = """
            FROM pos.bills b
            JOIN pos.orders o ON o.id = b.order_id
            WHERE b.tenant_uuid = :tenant AND b.is_deleted = false
            """;

    private final NamedParameterJdbcTemplate jdbc;

    // ---------------------------------------------------------------- one bill

    @Override
    public Optional<BillView> findBill(String billUuid) {
        return findOne(" AND b.uuid = :uuid", tenantParams().addValue("uuid", billUuid));
    }

    @Override
    public Optional<BillView> findIssuedBillOfOrder(String orderUuid) {
        return findOne(" AND o.uuid = :orderUuid AND b.status = 'ISSUED'",
                tenantParams().addValue("orderUuid", orderUuid));
    }

    private Optional<BillView> findOne(String where, MapSqlParameterSource params) {
        List<HeaderRow> rows = jdbc.query("SELECT b.*, o.uuid AS order_uuid, o.order_number " + BILL_FROM + where,
                params, HEADER_MAPPER);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        BillView h = rows.getFirst().header();
        params.addValue("billId", rows.getFirst().id());
        return Optional.of(new BillView(h.uuid(), h.billNumber(), h.billDate(), h.status(), h.orderUuid(),
                h.orderNumber(), h.seller(), h.buyer(), h.interState(), h.subTotal(), h.discountAmount(),
                h.taxableAmount(), h.taxAmount(), h.roundOffAmount(), h.grandTotal(), h.amountInWords(),
                lines(params), taxSummary(params), hsnSummary(params), h.printCount(), h.sharedVia(), h.issuedBy(),
                h.cancelledAt(), h.cancelledBy(), h.cancelReason()));
    }

    private List<BillLineView> lines(MapSqlParameterSource params) {
        Map<Long, List<BillLineTaxView>> taxes = new HashMap<>();
        jdbc.query("""
                SELECT t.bill_item_id, t.tax_type, t.tax_name, t.rate, t.taxable_amount, t.tax_amount
                FROM pos.bill_item_taxes t
                JOIN pos.bill_items i ON i.id = t.bill_item_id
                WHERE i.bill_id = :billId AND t.tenant_uuid = :tenant AND t.is_deleted = false
                ORDER BY t.id
                """, params, rs -> {
            taxes.computeIfAbsent(rs.getLong("bill_item_id"), k -> new ArrayList<>()).add(new BillLineTaxView(
                    TaxType.valueOf(rs.getString("tax_type")), rs.getString("tax_name"), rs.getBigDecimal("rate"),
                    rs.getBigDecimal("taxable_amount"), rs.getBigDecimal("tax_amount")));
        });
        return jdbc.query("""
                SELECT * FROM pos.bill_items
                WHERE bill_id = :billId AND tenant_uuid = :tenant AND is_deleted = false
                ORDER BY sort_order, id
                """, params, (rs, n) -> new BillLineView(rs.getString("uuid"), rs.getString("item_name"),
                rs.getString("addons_text"), rs.getString("hsn_sac_code"), rs.getString("unit_of_measure"),
                rs.getBigDecimal("quantity"), rs.getBigDecimal("unit_price"), rs.getBigDecimal("line_amount"),
                rs.getBigDecimal("discount_amount"), rs.getBigDecimal("tax_rate"), rs.getBigDecimal("taxable_amount"),
                rs.getBigDecimal("tax_amount"), rs.getBigDecimal("total_amount"),
                List.copyOf(taxes.getOrDefault(rs.getLong("id"), List.of()))));
    }

    private List<TaxSummaryView> taxSummary(MapSqlParameterSource params) {
        return jdbc.query("""
                SELECT t.tax_type, t.rate, sum(t.taxable_amount) AS taxable, sum(t.tax_amount) AS tax
                FROM pos.bill_item_taxes t
                JOIN pos.bill_items i ON i.id = t.bill_item_id
                WHERE i.bill_id = :billId AND t.tenant_uuid = :tenant AND t.is_deleted = false
                GROUP BY t.tax_type, t.rate
                ORDER BY t.rate, t.tax_type
                """, params, (rs, n) -> new TaxSummaryView(TaxType.valueOf(rs.getString("tax_type")),
                rs.getBigDecimal("rate"), rs.getBigDecimal("taxable"), rs.getBigDecimal("tax")));
    }

    private List<HsnSummaryView> hsnSummary(MapSqlParameterSource params) {
        return jdbc.query("""
                SELECT hsn_sac_code, tax_rate, sum(quantity) AS qty, sum(taxable_amount) AS taxable,
                       sum(tax_amount) AS tax, sum(total_amount) AS total
                FROM pos.bill_items
                WHERE bill_id = :billId AND tenant_uuid = :tenant AND is_deleted = false
                GROUP BY hsn_sac_code, tax_rate
                ORDER BY hsn_sac_code NULLS LAST, tax_rate
                """, params, (rs, n) -> new HsnSummaryView(rs.getString("hsn_sac_code"), rs.getBigDecimal("tax_rate"),
                rs.getBigDecimal("qty"), rs.getBigDecimal("taxable"), rs.getBigDecimal("tax"),
                rs.getBigDecimal("total")));
    }

    // ---------------------------------------------------------------- search

    @Override
    public PageResult<BillSummaryView> searchBills(SearchBillsQuery q) {
        var params = tenantParams();
        StringBuilder where = new StringBuilder();
        if (q.status() != null) {
            where.append(" AND b.status = :status");
            params.addValue("status", q.status().name());
        }
        if (q.from() != null) {
            where.append(" AND b.bill_date >= :from");
            params.addValue("from", q.from().atStartOfDay());
        }
        if (q.to() != null) {
            where.append(" AND b.bill_date < :to");
            params.addValue("to", q.to().plusDays(1).atStartOfDay());
        }
        if (q.search() != null && !q.search().isBlank()) {
            String term = q.search().trim();
            where.append("""
                     AND (b.bill_number ILIKE :like ESCAPE '\\' OR o.order_number = :term
                          OR b.customer_phone LIKE :like ESCAPE '\\' OR b.customer_gstin ILIKE :like ESCAPE '\\')
                    """);
            params.addValue("term", term).addValue("like", "%" + escapeLike(term) + "%");
        }

        Long total = jdbc.queryForObject("SELECT count(*) " + BILL_FROM + where, params, Long.class);
        params.addValue("limit", q.size()).addValue("offset", (long) q.page() * q.size());
        List<BillSummaryView> rows = jdbc.query("""
                SELECT b.uuid, b.bill_number, b.bill_date, b.status, o.uuid AS order_uuid, o.order_number,
                       b.customer_name, b.customer_phone, b.customer_gstin, b.taxable_amount, b.tax_amount,
                       b.grand_total, b.print_count
                """ + BILL_FROM + where + " ORDER BY b.bill_date DESC, b.id DESC LIMIT :limit OFFSET :offset",
                params, (rs, n) -> new BillSummaryView(rs.getString("uuid"), rs.getString("bill_number"),
                        toLocalDateTime(rs.getTimestamp("bill_date")), BillStatus.valueOf(rs.getString("status")),
                        rs.getString("order_uuid"), rs.getString("order_number"), rs.getString("customer_name"),
                        rs.getString("customer_phone"), rs.getString("customer_gstin"),
                        rs.getBigDecimal("taxable_amount"), rs.getBigDecimal("tax_amount"),
                        rs.getBigDecimal("grand_total"), rs.getInt("print_count")));
        return PageResult.of(rows, q.page(), q.size(), total == null ? 0 : total);
    }

    // ---------------------------------------------------------------- mapping

    /** Header only; lines and summaries are filled in by {@link #findOne}. */
    private record HeaderRow(long id, BillView header) {
    }

    private static final RowMapper<HeaderRow> HEADER_MAPPER = (rs, n) -> {
        String sellerState = rs.getString("seller_state_code");
        String place = rs.getString("place_of_supply");
        return new HeaderRow(rs.getLong("id"), new BillView(rs.getString("uuid"), rs.getString("bill_number"),
                toLocalDateTime(rs.getTimestamp("bill_date")), BillStatus.valueOf(rs.getString("status")),
                rs.getString("order_uuid"), rs.getString("order_number"),
                new BillView.Seller(rs.getString("seller_name"), rs.getString("seller_address"),
                        rs.getString("seller_gstin"), sellerState),
                new BillView.Buyer(rs.getString("customer_name"), rs.getString("customer_phone"),
                        rs.getString("customer_gstin"), place),
                place != null && sellerState != null && !place.equals(sellerState),
                rs.getBigDecimal("sub_total"), rs.getBigDecimal("discount_amount"), rs.getBigDecimal("taxable_amount"),
                rs.getBigDecimal("tax_amount"), rs.getBigDecimal("round_off_amount"), rs.getBigDecimal("grand_total"),
                AmountInWords.of(Money.of(rs.getBigDecimal("grand_total"))), null, null, null,
                rs.getInt("print_count"), enumOrNull(ShareChannel.class, rs.getString("shared_via")),
                rs.getString("issued_by"), toLocalDateTime(rs.getTimestamp("cancelled_at")),
                rs.getString("cancelled_by"), rs.getString("cancel_reason")));
    };

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
