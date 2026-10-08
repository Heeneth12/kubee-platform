package com.kubee.pos.reporting.infrastructure.persistence;

import com.kubee.pos.common.tenant.TenantContext;
import com.kubee.pos.reporting.application.query.CancellationReport;
import com.kubee.pos.reporting.application.query.ChannelSalesReport;
import com.kubee.pos.reporting.application.query.GstReport;
import com.kubee.pos.reporting.application.query.Gstr1Builder;
import com.kubee.pos.reporting.application.query.Gstr1Data;
import com.kubee.pos.reporting.application.query.StaffSalesReport;
import com.kubee.pos.reporting.application.query.ShiftReport;
import com.kubee.pos.reporting.application.query.HourlySalesReport;
import com.kubee.pos.reporting.application.query.CategorySalesReport;
import com.kubee.pos.reporting.application.query.ItemSalesQuery;
import com.kubee.pos.reporting.application.query.ItemSalesReport;
import com.kubee.pos.reporting.application.query.PaymentModeReport;
import com.kubee.pos.reporting.application.query.ReportPeriod;
import com.kubee.pos.reporting.application.query.ReportQueryRepository;
import com.kubee.pos.reporting.application.query.SalesSummaryReport;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * All reports are plain SQL over the shared tables (orders, payments, bills), filtered by tenant and period.
 * Timestamps are stored as shop-local time, so day boundaries are midnight in the shop's time zone.
 */
@Repository
@RequiredArgsConstructor
class JdbcReportQueryRepository implements ReportQueryRepository {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** Orders completed in the period. */
    private static final String COMPLETED_ORDERS = """
            FROM pos.orders o
            WHERE o.tenant_uuid = :tenant AND o.is_deleted = false AND o.status = 'COMPLETED'
              AND o.completed_at >= :start AND o.completed_at < :end
            """;

    /** Lines of ISSUED bills dated in the period, with their tax split pivoted per line (one row per line). */
    private static final String BILLED_LINES = """
            FROM pos.bills b
            JOIN pos.bill_items i ON i.bill_id = b.id AND i.is_deleted = false
            LEFT JOIN (
                SELECT bill_item_id,
                       sum(tax_amount) FILTER (WHERE tax_type = 'CGST') AS cgst,
                       sum(tax_amount) FILTER (WHERE tax_type = 'SGST') AS sgst,
                       sum(tax_amount) FILTER (WHERE tax_type = 'IGST') AS igst,
                       sum(tax_amount) FILTER (WHERE tax_type IN ('CESS', 'OTHER')) AS cess
                FROM pos.bill_item_taxes
                WHERE tenant_uuid = :tenant AND is_deleted = false
                GROUP BY bill_item_id
            ) t ON t.bill_item_id = i.id
            WHERE b.tenant_uuid = :tenant AND b.is_deleted = false AND b.status = 'ISSUED'
              AND b.bill_date >= :start AND b.bill_date < :end
            """;

    private static final String TAX_SUMS = """
            coalesce(sum(i.taxable_amount), 0) AS taxable, coalesce(sum(t.cgst), 0) AS cgst,
            coalesce(sum(t.sgst), 0) AS sgst, coalesce(sum(t.igst), 0) AS igst, coalesce(sum(t.cess), 0) AS cess
            """;

    private final NamedParameterJdbcTemplate jdbc;

    // ---------------------------------------------------------------- 1. sales summary

    @Override
    public SalesSummaryReport salesSummary(ReportPeriod period) {
        var params = params(period);
        Map<String, Object> sales = jdbc.queryForMap("""
                SELECT count(*) AS orders, coalesce(sum(o.sub_total), 0) AS gross,
                       coalesce(sum(o.discount_amount), 0) AS discount, coalesce(sum(o.taxable_amount), 0) AS taxable,
                       coalesce(sum(o.tax_amount), 0) AS tax, coalesce(sum(o.round_off_amount), 0) AS round_off,
                       coalesce(sum(o.grand_total), 0) AS net
                """ + COMPLETED_ORDERS, params);
        Map<String, Object> money = jdbc.queryForMap("""
                SELECT coalesce(sum(amount) FILTER (WHERE payment_type = 'PAYMENT'), 0) AS collected,
                       coalesce(sum(amount) FILTER (WHERE payment_type = 'REFUND'), 0) AS refunded
                FROM pos.payments
                WHERE tenant_uuid = :tenant AND is_deleted = false AND status = 'SUCCESS'
                  AND paid_at >= :start AND paid_at < :end
                """, params);
        Map<String, Object> counts = jdbc.queryForMap("""
                SELECT (SELECT count(*) FROM pos.orders WHERE tenant_uuid = :tenant AND is_deleted = false
                          AND status = 'CANCELLED' AND cancelled_at >= :start AND cancelled_at < :end) AS cancelled_orders,
                       (SELECT count(*) FROM pos.bills WHERE tenant_uuid = :tenant AND is_deleted = false
                          AND bill_date >= :start AND bill_date < :end) AS bills_issued,
                       (SELECT count(*) FROM pos.bills WHERE tenant_uuid = :tenant AND is_deleted = false
                          AND status = 'CANCELLED' AND cancelled_at >= :start AND cancelled_at < :end) AS bills_cancelled
                """, params);
        List<SalesSummaryReport.Day> days = jdbc.query("""
                SELECT o.completed_at::date AS day, count(*) AS orders, sum(o.grand_total) AS net,
                       sum(o.tax_amount) AS tax
                """ + COMPLETED_ORDERS + " GROUP BY day ORDER BY day", params,
                (rs, n) -> new SalesSummaryReport.Day(rs.getDate("day").toLocalDate(), rs.getLong("orders"),
                        rs.getBigDecimal("net"), rs.getBigDecimal("tax"),
                        average(rs.getBigDecimal("net"), rs.getLong("orders"))));

        long orders = ((Number) sales.get("orders")).longValue();
        BigDecimal collected = money(money.get("collected"));
        BigDecimal refunded = money(money.get("refunded"));
        return new SalesSummaryReport(period.from(), period.to(), orders, money(sales.get("gross")),
                money(sales.get("discount")), money(sales.get("taxable")), money(sales.get("tax")),
                money(sales.get("round_off")), money(sales.get("net")), average(money(sales.get("net")), orders),
                collected, refunded, collected.subtract(refunded),
                count(counts.get("cancelled_orders")), count(counts.get("bills_issued")),
                count(counts.get("bills_cancelled")), days);
    }

    // ---------------------------------------------------------------- 2. payment modes

    @Override
    public PaymentModeReport paymentModes(ReportPeriod period) {
        List<PaymentModeReport.Row> rows = jdbc.query("""
                SELECT payment_method,
                       count(*) FILTER (WHERE payment_type = 'PAYMENT') AS payments,
                       coalesce(sum(amount) FILTER (WHERE payment_type = 'PAYMENT'), 0) AS paid,
                       count(*) FILTER (WHERE payment_type = 'REFUND') AS refunds,
                       coalesce(sum(amount) FILTER (WHERE payment_type = 'REFUND'), 0) AS refunded
                FROM pos.payments
                WHERE tenant_uuid = :tenant AND is_deleted = false AND status = 'SUCCESS'
                  AND paid_at >= :start AND paid_at < :end
                GROUP BY payment_method
                ORDER BY paid DESC, payment_method
                """, params(period), (rs, n) -> new PaymentModeReport.Row(rs.getString("payment_method"),
                rs.getLong("payments"), rs.getBigDecimal("paid"), rs.getLong("refunds"), rs.getBigDecimal("refunded"),
                rs.getBigDecimal("paid").subtract(rs.getBigDecimal("refunded"))));

        var total = new PaymentModeReport.Row(null,
                rows.stream().mapToLong(PaymentModeReport.Row::paymentCount).sum(),
                sum(rows.stream().map(PaymentModeReport.Row::paymentAmount).toList()),
                rows.stream().mapToLong(PaymentModeReport.Row::refundCount).sum(),
                sum(rows.stream().map(PaymentModeReport.Row::refundAmount).toList()),
                sum(rows.stream().map(PaymentModeReport.Row::netAmount).toList()));
        return new PaymentModeReport(period.from(), period.to(), rows, total);
    }

    // ---------------------------------------------------------------- 3. item-wise sales

    @Override
    public ItemSalesReport itemSales(ItemSalesQuery query) {
        var params = params(query.period());
        String categoryFilter = "";
        if (query.categoryUuid() != null && !query.categoryUuid().isBlank()) {
            categoryFilter = " AND c.uuid = :categoryUuid";
            params.addValue("categoryUuid", query.categoryUuid().trim());
        }
        String orderBy = query.sort() == ItemSalesQuery.Sort.QUANTITY
                ? " ORDER BY quantity DESC, net DESC" : " ORDER BY net DESC, quantity DESC";

        record Line(String itemUuid, String itemName, String variantUuid, String variantName, String category,
                    String unit, BigDecimal quantity, long orders, BigDecimal gross, BigDecimal discount,
                    BigDecimal taxable, BigDecimal tax, BigDecimal net) {
        }
        List<Line> lines = jdbc.query("""
                SELECT i.uuid AS item_uuid, coalesce(i.name, max(l.item_name)) AS item_name,
                       v.uuid AS variant_uuid, coalesce(v.name, max(l.variant_name)) AS variant_name,
                       c.name AS category_name, max(l.unit_of_measure) AS unit,
                       sum(l.quantity) AS quantity, count(DISTINCT l.order_id) AS orders,
                       sum(l.line_amount) AS gross, sum(l.discount_amount) AS discount,
                       sum(l.taxable_amount) AS taxable, sum(l.tax_amount) AS tax, sum(l.total_amount) AS net
                FROM pos.order_items l
                JOIN pos.orders o ON o.id = l.order_id
                LEFT JOIN pos.items i ON i.id = l.item_id
                LEFT JOIN pos.item_variants v ON v.id = l.item_variant_id
                LEFT JOIN pos.categories c ON c.id = i.category_id AND c.is_deleted = false
                WHERE o.tenant_uuid = :tenant AND o.is_deleted = false AND o.status = 'COMPLETED'
                  AND o.completed_at >= :start AND o.completed_at < :end AND l.is_deleted = false
                """ + categoryFilter + """

                GROUP BY l.item_id, l.item_variant_id, i.uuid, i.name, v.uuid, v.name, c.name
                """ + orderBy, params, (rs, n) -> new Line(rs.getString("item_uuid"), rs.getString("item_name"),
                rs.getString("variant_uuid"), rs.getString("variant_name"), rs.getString("category_name"),
                rs.getString("unit"), rs.getBigDecimal("quantity"), rs.getLong("orders"), rs.getBigDecimal("gross"),
                rs.getBigDecimal("discount"), rs.getBigDecimal("taxable"), rs.getBigDecimal("tax"),
                rs.getBigDecimal("net")));

        BigDecimal total = sum(lines.stream().map(Line::net).toList());
        List<ItemSalesReport.Row> rows = lines.stream().map(l -> new ItemSalesReport.Row(l.itemUuid(), l.itemName(),
                l.variantUuid(), l.variantName(), l.category(), l.unit(), l.quantity(), l.orders(), l.gross(),
                l.discount(), l.taxable(), l.tax(), l.net(),
                share(l.net(), total)))
                .toList();
        return new ItemSalesReport(query.period().from(), query.period().to(), total, rows);
    }

    // ---------------------------------------------------------------- 4. GST

    @Override
    public GstReport gstSummary(ReportPeriod period) {
        var params = params(period);

        Map<String, Object> head = jdbc.queryForMap("""
                SELECT count(*) AS invoices, coalesce(sum(round_off_amount), 0) AS round_off,
                       coalesce(sum(grand_total), 0) AS invoice_value
                FROM pos.bills b
                WHERE b.tenant_uuid = :tenant AND b.is_deleted = false AND b.status = 'ISSUED'
                  AND b.bill_date >= :start AND b.bill_date < :end
                """, params);
        Map<String, Object> tax = jdbc.queryForMap("SELECT " + TAX_SUMS + BILLED_LINES, params);
        BigDecimal cgst = money(tax.get("cgst"));
        BigDecimal sgst = money(tax.get("sgst"));
        BigDecimal igst = money(tax.get("igst"));
        BigDecimal cess = money(tax.get("cess"));
        var totals = new GstReport.Totals(count(head.get("invoices")), money(tax.get("taxable")), cgst, sgst, igst,
                cess, cgst.add(sgst).add(igst).add(cess), money(head.get("round_off")),
                money(head.get("invoice_value")));

        List<GstReport.RateRow> byRate = jdbc.query(
                "SELECT i.tax_rate, " + TAX_SUMS + ", sum(i.total_amount) AS value " + BILLED_LINES
                        + " GROUP BY i.tax_rate ORDER BY i.tax_rate", params,
                (rs, n) -> new GstReport.RateRow(rs.getBigDecimal("tax_rate"), rs.getBigDecimal("taxable"),
                        rs.getBigDecimal("cgst"), rs.getBigDecimal("sgst"), rs.getBigDecimal("igst"),
                        rs.getBigDecimal("cess"), totalTax(rs), rs.getBigDecimal("value")));

        List<GstReport.B2bInvoice> b2b = jdbc.query("SELECT b.bill_number, b.bill_date, b.customer_gstin, "
                        + "b.customer_name, b.place_of_supply, b.grand_total, " + TAX_SUMS + BILLED_LINES
                        + " AND b.customer_gstin IS NOT NULL GROUP BY b.id ORDER BY b.bill_date, b.id", params,
                (rs, n) -> new GstReport.B2bInvoice(rs.getString("bill_number"), toLocalDateTime(rs.getTimestamp("bill_date")),
                        rs.getString("customer_gstin"), rs.getString("customer_name"), rs.getString("place_of_supply"),
                        rs.getBigDecimal("taxable"), rs.getBigDecimal("cgst"), rs.getBigDecimal("sgst"),
                        rs.getBigDecimal("igst"), rs.getBigDecimal("cess"), rs.getBigDecimal("grand_total")));

        List<GstReport.B2cRow> b2c = jdbc.query("SELECT b.place_of_supply, i.tax_rate, " + TAX_SUMS + BILLED_LINES
                        + " AND b.customer_gstin IS NULL GROUP BY b.place_of_supply, i.tax_rate"
                        + " ORDER BY b.place_of_supply NULLS FIRST, i.tax_rate", params,
                (rs, n) -> new GstReport.B2cRow(rs.getString("place_of_supply"), rs.getBigDecimal("tax_rate"),
                        rs.getBigDecimal("taxable"), rs.getBigDecimal("cgst"), rs.getBigDecimal("sgst"),
                        rs.getBigDecimal("igst"), rs.getBigDecimal("cess")));

        List<GstReport.HsnRow> hsn = jdbc.query("SELECT i.hsn_sac_code, i.unit_of_measure, i.tax_rate, "
                        + "sum(i.quantity) AS quantity, sum(i.total_amount) AS value, " + TAX_SUMS + BILLED_LINES
                        + " GROUP BY i.hsn_sac_code, i.unit_of_measure, i.tax_rate"
                        + " ORDER BY i.hsn_sac_code NULLS LAST, i.tax_rate, i.unit_of_measure", params,
                (rs, n) -> new GstReport.HsnRow(rs.getString("hsn_sac_code"), rs.getString("unit_of_measure"),
                        rs.getBigDecimal("tax_rate"), rs.getBigDecimal("quantity"), rs.getBigDecimal("taxable"),
                        rs.getBigDecimal("cgst"), rs.getBigDecimal("sgst"), rs.getBigDecimal("igst"),
                        rs.getBigDecimal("cess"), rs.getBigDecimal("value")));

        Map<String, Object> gaps = jdbc.queryForMap("""
                SELECT (SELECT count(*) """ + COMPLETED_ORDERS + """
                         AND NOT EXISTS (SELECT 1 FROM pos.bills x WHERE x.order_id = o.id AND x.status = 'ISSUED'
                                         AND x.is_deleted = false)) AS unbilled,
                       (SELECT count(*) FROM pos.bills WHERE tenant_uuid = :tenant AND is_deleted = false
                          AND status = 'CANCELLED' AND bill_date >= :start AND bill_date < :end) AS cancelled
                """, params);

        return new GstReport(period.from(), period.to(), totals, byRate, b2b, b2c, hsn,
                count(gaps.get("unbilled")), count(gaps.get("cancelled")));
    }

    // ---------------------------------------------------------------- 5. cancellations and refunds

    @Override
    public CancellationReport cancellations(ReportPeriod period) {
        var params = params(period);
        List<CancellationReport.CancelledOrder> orders = jdbc.query("""
                SELECT o.uuid, o.order_number, o.created_at, o.cancelled_at, o.cancelled_by, o.cancel_reason,
                       o.grand_total,
                       (SELECT count(*) FROM pos.order_items l WHERE l.order_id = o.id AND l.is_deleted = false) AS lines
                FROM pos.orders o
                WHERE o.tenant_uuid = :tenant AND o.is_deleted = false AND o.status = 'CANCELLED'
                  AND o.cancelled_at >= :start AND o.cancelled_at < :end
                ORDER BY o.cancelled_at DESC
                """, params, (rs, n) -> new CancellationReport.CancelledOrder(rs.getString("uuid"),
                rs.getString("order_number"), toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("cancelled_at")), rs.getString("cancelled_by"),
                rs.getString("cancel_reason"), rs.getInt("lines"), rs.getBigDecimal("grand_total")));

        List<CancellationReport.CancelledBill> bills = jdbc.query("""
                SELECT b.uuid, b.bill_number, o.order_number, b.bill_date, b.cancelled_at, b.cancelled_by,
                       b.cancel_reason, b.grand_total
                FROM pos.bills b
                JOIN pos.orders o ON o.id = b.order_id
                WHERE b.tenant_uuid = :tenant AND b.is_deleted = false AND b.status = 'CANCELLED'
                  AND b.cancelled_at >= :start AND b.cancelled_at < :end
                ORDER BY b.cancelled_at DESC
                """, params, (rs, n) -> new CancellationReport.CancelledBill(rs.getString("uuid"),
                rs.getString("bill_number"), rs.getString("order_number"), toLocalDateTime(rs.getTimestamp("bill_date")),
                toLocalDateTime(rs.getTimestamp("cancelled_at")), rs.getString("cancelled_by"),
                rs.getString("cancel_reason"), rs.getBigDecimal("grand_total")));

        List<CancellationReport.Refund> refunds = jdbc.query("""
                SELECT p.uuid, o.uuid AS order_uuid, o.order_number, p.payment_method, p.amount, p.notes,
                       p.paid_at, p.received_by
                FROM pos.payments p
                JOIN pos.orders o ON o.id = p.order_id
                WHERE p.tenant_uuid = :tenant AND p.is_deleted = false AND p.status = 'SUCCESS'
                  AND p.payment_type = 'REFUND' AND p.paid_at >= :start AND p.paid_at < :end
                ORDER BY p.paid_at DESC
                """, params, (rs, n) -> new CancellationReport.Refund(rs.getString("uuid"),
                rs.getString("order_uuid"), rs.getString("order_number"), rs.getString("payment_method"),
                rs.getBigDecimal("amount"), rs.getString("notes"), toLocalDateTime(rs.getTimestamp("paid_at")),
                rs.getString("received_by")));

        return new CancellationReport(period.from(), period.to(),
                orders.size(), sum(orders.stream().map(CancellationReport.CancelledOrder::orderValue).toList()),
                bills.size(), sum(bills.stream().map(CancellationReport.CancelledBill::billValue).toList()),
                refunds.size(), sum(refunds.stream().map(CancellationReport.Refund::amount).toList()),
                orders, bills, refunds);
    }

    // ---------------------------------------------------------------- hourly

    @Override
    public HourlySalesReport hourlySales(ReportPeriod period) {
        Map<Integer, HourlySalesReport.Hour> byHour = new HashMap<>();
        jdbc.query("""
                SELECT extract(HOUR FROM o.completed_at)::int AS hour, count(*) AS orders, sum(o.grand_total) AS net
                """ + COMPLETED_ORDERS + " GROUP BY hour", params(period), rs -> {
            int hour = rs.getInt("hour");
            byHour.put(hour, new HourlySalesReport.Hour(hour, rs.getLong("orders"), rs.getBigDecimal("net"),
                    average(rs.getBigDecimal("net"), rs.getLong("orders"))));
        });
        List<HourlySalesReport.Hour> hours = IntStream.range(0, 24)
                .mapToObj(h -> byHour.getOrDefault(h, new HourlySalesReport.Hour(h, 0, ZERO, ZERO)))
                .toList();
        return new HourlySalesReport(period.from(), period.to(), hours);
    }

    // ---------------------------------------------------------------- staff

    @Override
    public StaffSalesReport staffSales(ReportPeriod period) {
        List<StaffSalesReport.Row> rows = jdbc.query("""
                WITH sales AS (
                    SELECT o.created_by AS user_uuid, count(*) AS orders, sum(o.grand_total) AS net,
                           sum(o.discount_amount) AS discount
                    """ + COMPLETED_ORDERS + """
                    GROUP BY o.created_by
                ), money AS (
                    SELECT received_by AS user_uuid,
                           count(*) FILTER (WHERE payment_type = 'PAYMENT') AS payments,
                           coalesce(sum(amount) FILTER (WHERE payment_type = 'PAYMENT'), 0) AS paid,
                           count(*) FILTER (WHERE payment_type = 'REFUND') AS refunds,
                           coalesce(sum(amount) FILTER (WHERE payment_type = 'REFUND'), 0) AS refunded
                    FROM pos.payments
                    WHERE tenant_uuid = :tenant AND is_deleted = false AND status = 'SUCCESS'
                      AND paid_at >= :start AND paid_at < :end
                    GROUP BY received_by
                ), cancelled_orders AS (
                    SELECT cancelled_by AS user_uuid, count(*) AS n FROM pos.orders
                    WHERE tenant_uuid = :tenant AND is_deleted = false AND status = 'CANCELLED'
                      AND cancelled_at >= :start AND cancelled_at < :end
                    GROUP BY cancelled_by
                ), cancelled_bills AS (
                    SELECT cancelled_by AS user_uuid, count(*) AS n FROM pos.bills
                    WHERE tenant_uuid = :tenant AND is_deleted = false AND status = 'CANCELLED'
                      AND cancelled_at >= :start AND cancelled_at < :end
                    GROUP BY cancelled_by
                ), users AS (
                    SELECT user_uuid FROM sales UNION SELECT user_uuid FROM money
                    UNION SELECT user_uuid FROM cancelled_orders UNION SELECT user_uuid FROM cancelled_bills
                )
                SELECT u.user_uuid, coalesce(s.orders, 0) AS orders, coalesce(s.net, 0) AS net,
                       coalesce(s.discount, 0) AS discount, coalesce(m.payments, 0) AS payments,
                       coalesce(m.paid, 0) AS paid, coalesce(m.refunds, 0) AS refunds,
                       coalesce(m.refunded, 0) AS refunded, coalesce(co.n, 0) AS cancelled_orders,
                       coalesce(cb.n, 0) AS cancelled_bills
                FROM users u
                LEFT JOIN sales s ON s.user_uuid IS NOT DISTINCT FROM u.user_uuid
                LEFT JOIN money m ON m.user_uuid IS NOT DISTINCT FROM u.user_uuid
                LEFT JOIN cancelled_orders co ON co.user_uuid IS NOT DISTINCT FROM u.user_uuid
                LEFT JOIN cancelled_bills cb ON cb.user_uuid IS NOT DISTINCT FROM u.user_uuid
                ORDER BY net DESC, paid DESC
                """, params(period), (rs, n) -> new StaffSalesReport.Row(rs.getString("user_uuid"),
                rs.getLong("orders"), money(rs.getBigDecimal("net")), money(rs.getBigDecimal("discount")),
                rs.getLong("payments"), money(rs.getBigDecimal("paid")), rs.getLong("refunds"),
                money(rs.getBigDecimal("refunded")), rs.getLong("cancelled_orders"), rs.getLong("cancelled_bills")));
        return new StaffSalesReport(period.from(), period.to(), rows);
    }

    // ---------------------------------------------------------------- channels (POS / Zomato / Swiggy)

    @Override
    public ChannelSalesReport channelSales(ReportPeriod period) {
        record Line(String source, long orders, BigDecimal gross, BigDecimal discount, BigDecimal tax,
                    BigDecimal net, long cancelled) {
        }
        List<Line> lines = jdbc.query("""
                WITH sales AS (
                    SELECT o.source, count(*) AS orders, sum(o.sub_total) AS gross,
                           sum(o.discount_amount) AS discount, sum(o.tax_amount) AS tax, sum(o.grand_total) AS net
                    """ + COMPLETED_ORDERS + """
                    GROUP BY o.source
                ), cancelled AS (
                    SELECT source, count(*) AS n FROM pos.orders
                    WHERE tenant_uuid = :tenant AND is_deleted = false AND status = 'CANCELLED'
                      AND cancelled_at >= :start AND cancelled_at < :end
                    GROUP BY source
                )
                SELECT coalesce(s.source, c.source) AS source, coalesce(s.orders, 0) AS orders,
                       coalesce(s.gross, 0) AS gross, coalesce(s.discount, 0) AS discount,
                       coalesce(s.tax, 0) AS tax, coalesce(s.net, 0) AS net, coalesce(c.n, 0) AS cancelled
                FROM sales s
                FULL JOIN cancelled c ON c.source = s.source
                ORDER BY net DESC, source
                """, params(period), (rs, n) -> new Line(rs.getString("source"), rs.getLong("orders"),
                money(rs.getBigDecimal("gross")), money(rs.getBigDecimal("discount")), money(rs.getBigDecimal("tax")),
                money(rs.getBigDecimal("net")), rs.getLong("cancelled")));

        BigDecimal totalNet = sum(lines.stream().map(Line::net).toList());
        List<ChannelSalesReport.Row> rows = lines.stream().map(l -> new ChannelSalesReport.Row(l.source(),
                l.orders(), l.gross(), l.discount(), l.tax(), l.net(), average(l.net(), l.orders()),
                share(l.net(), totalNet), l.cancelled())).toList();
        long totalOrders = lines.stream().mapToLong(Line::orders).sum();
        var total = new ChannelSalesReport.Row(null, totalOrders, sum(lines.stream().map(Line::gross).toList()),
                sum(lines.stream().map(Line::discount).toList()), sum(lines.stream().map(Line::tax).toList()),
                totalNet, average(totalNet, totalOrders), totalNet.signum() == 0 ? ZERO : HUNDRED.setScale(2),
                lines.stream().mapToLong(Line::cancelled).sum());
        return new ChannelSalesReport(period.from(), period.to(), rows, total);
    }

    // ---------------------------------------------------------------- categories

    @Override
    public CategorySalesReport categorySales(ReportPeriod period) {
        record Line(String uuid, String name, String parent, BigDecimal quantity, long orders, BigDecimal gross,
                    BigDecimal discount, BigDecimal taxable, BigDecimal tax, BigDecimal net) {
        }
        List<Line> lines = jdbc.query("""
                SELECT c.uuid AS category_uuid, c.name AS category_name, p.name AS parent_name,
                       sum(l.quantity) AS quantity, count(DISTINCT l.order_id) AS orders,
                       sum(l.line_amount) AS gross, sum(l.discount_amount) AS discount,
                       sum(l.taxable_amount) AS taxable, sum(l.tax_amount) AS tax, sum(l.total_amount) AS net
                FROM pos.order_items l
                JOIN pos.orders o ON o.id = l.order_id
                LEFT JOIN pos.items i ON i.id = l.item_id
                LEFT JOIN pos.categories c ON c.id = i.category_id AND c.is_deleted = false
                LEFT JOIN pos.categories p ON p.id = c.parent_id AND p.is_deleted = false
                WHERE o.tenant_uuid = :tenant AND o.is_deleted = false AND o.status = 'COMPLETED'
                  AND o.completed_at >= :start AND o.completed_at < :end AND l.is_deleted = false
                GROUP BY c.uuid, c.name, p.name
                ORDER BY net DESC
                """, params(period), (rs, n) -> new Line(rs.getString("category_uuid"), rs.getString("category_name"),
                rs.getString("parent_name"), rs.getBigDecimal("quantity"), rs.getLong("orders"),
                rs.getBigDecimal("gross"), rs.getBigDecimal("discount"), rs.getBigDecimal("taxable"),
                rs.getBigDecimal("tax"), rs.getBigDecimal("net")));
        BigDecimal total = sum(lines.stream().map(Line::net).toList());
        List<CategorySalesReport.Row> rows = lines.stream().map(l -> new CategorySalesReport.Row(l.uuid(),
                l.name() == null ? "Uncategorised" : l.name(), l.parent(), l.quantity(), l.orders(), l.gross(),
                l.discount(), l.taxable(), l.tax(), l.net(), share(l.net(), total))).toList();
        return new CategorySalesReport(period.from(), period.to(), total, rows);
    }

    // ---------------------------------------------------------------- shifts

    @Override
    public ShiftReport shifts(ReportPeriod period) {
        List<ShiftReport.Row> rows = jdbc.query("""
                SELECT s.*,
                       coalesce(s.cash_sales, (
                           SELECT coalesce(sum(CASE WHEN p.payment_type = 'PAYMENT' THEN p.amount ELSE -p.amount END), 0)
                           FROM pos.payments p
                           WHERE p.tenant_uuid = :tenant AND p.is_deleted = false AND p.status = 'SUCCESS'
                             AND p.payment_method = 'CASH' AND p.paid_at >= s.opened_at)) AS live_cash_sales,
                       coalesce(s.cash_in, (SELECT coalesce(sum(m.amount), 0) FROM pos.cash_movements m
                           WHERE m.shift_id = s.id AND m.movement_type = 'IN' AND m.is_deleted = false)) AS live_cash_in,
                       coalesce(s.cash_out, (SELECT coalesce(sum(m.amount), 0) FROM pos.cash_movements m
                           WHERE m.shift_id = s.id AND m.movement_type = 'OUT' AND m.is_deleted = false)) AS live_cash_out
                FROM pos.shifts s
                WHERE s.tenant_uuid = :tenant AND s.is_deleted = false
                  AND s.opened_at >= :start AND s.opened_at < :end
                ORDER BY s.opened_at DESC
                """, params(period), (rs, n) -> {
            BigDecimal cashSales = rs.getBigDecimal("live_cash_sales");
            BigDecimal cashIn = rs.getBigDecimal("live_cash_in");
            BigDecimal cashOut = rs.getBigDecimal("live_cash_out");
            BigDecimal expected = rs.getBigDecimal("expected_cash") != null ? rs.getBigDecimal("expected_cash")
                    : rs.getBigDecimal("opening_cash").add(cashSales).add(cashIn).subtract(cashOut);
            return new ShiftReport.Row(rs.getString("uuid"), rs.getString("status"),
                    toLocalDateTime(rs.getTimestamp("opened_at")), rs.getString("opened_by"),
                    toLocalDateTime(rs.getTimestamp("closed_at")), rs.getString("closed_by"),
                    rs.getBigDecimal("opening_cash"), money(cashSales), money(cashIn), money(cashOut), money(expected),
                    rs.getBigDecimal("counted_cash"), rs.getBigDecimal("cash_difference"),
                    rs.getString("closing_notes"));
        });
        BigDecimal shortTotal = sum(rows.stream().map(ShiftReport.Row::cashDifference)
                .filter(d -> d != null && d.signum() < 0).map(BigDecimal::negate).toList());
        BigDecimal excessTotal = sum(rows.stream().map(ShiftReport.Row::cashDifference)
                .filter(d -> d != null && d.signum() > 0).toList());
        return new ShiftReport(period.from(), period.to(), rows.size(), shortTotal, excessTotal, rows);
    }

    // ---------------------------------------------------------------- GSTR-1

    @Override
    public Gstr1Data gstr1(YearMonth month) {
        var period = new ReportPeriod(month.atDay(1), month.atEndOfMonth());
        var params = params(period).addValue("b2clLimit", Gstr1Builder.B2CL_LIMIT);
        Map<String, Object> seller = jdbc.queryForList("""
                SELECT gstin, state_code FROM pos.pos_settings WHERE tenant_uuid = :tenant AND is_deleted = false
                """, params).stream().findFirst().orElse(Map.of());
        String gstin = (String) seller.get("gstin");
        String state = seller.get("state_code") != null ? (String) seller.get("state_code")
                : gstin == null ? null : gstin.substring(0, 2);
        params.addValue("state", state);

        String taxed = " AND i.tax_rate > 0";
        String b2cl = " AND b.customer_gstin IS NULL AND b.place_of_supply IS DISTINCT FROM :state"
                + " AND b.grand_total > :b2clLimit";
        String invoiceRates = "SELECT b.bill_number, b.bill_date, b.customer_gstin, b.place_of_supply, b.grand_total, "
                + "i.tax_rate, " + TAX_SUMS + BILLED_LINES;
        RowMapper<Gstr1Data.InvoiceRate> invoiceRate = (rs, n) -> new Gstr1Data.InvoiceRate(
                rs.getString("bill_number"), rs.getTimestamp("bill_date").toLocalDateTime().toLocalDate(),
                rs.getString("customer_gstin"), rs.getString("place_of_supply"), rs.getBigDecimal("grand_total"),
                rs.getBigDecimal("tax_rate"), rs.getBigDecimal("taxable"), rs.getBigDecimal("igst"),
                rs.getBigDecimal("cgst"), rs.getBigDecimal("sgst"), rs.getBigDecimal("cess"));
        String byInvoiceRate = " GROUP BY b.id, i.tax_rate ORDER BY b.bill_date, b.id, i.tax_rate";

        List<Gstr1Data.InvoiceRate> b2b = jdbc.query(invoiceRates + taxed + " AND b.customer_gstin IS NOT NULL"
                + byInvoiceRate, params, invoiceRate);
        List<Gstr1Data.InvoiceRate> b2clRows = jdbc.query(invoiceRates + taxed + b2cl + byInvoiceRate, params,
                invoiceRate);

        List<Gstr1Data.B2cs> b2cs = jdbc.query("SELECT b.place_of_supply, i.tax_rate, " + TAX_SUMS + BILLED_LINES
                        + taxed + " AND b.customer_gstin IS NULL AND NOT (b.place_of_supply IS DISTINCT FROM :state"
                        + " AND b.grand_total > :b2clLimit) GROUP BY b.place_of_supply, i.tax_rate"
                        + " ORDER BY b.place_of_supply, i.tax_rate", params,
                (rs, n) -> {
                    String pos = rs.getString("place_of_supply") == null ? state : rs.getString("place_of_supply");
                    return new Gstr1Data.B2cs(pos, pos == null || pos.equals(state), rs.getBigDecimal("tax_rate"),
                            rs.getBigDecimal("taxable"), rs.getBigDecimal("igst"), rs.getBigDecimal("cgst"),
                            rs.getBigDecimal("sgst"), rs.getBigDecimal("cess"));
                });

        List<Gstr1Data.Nil> nil = jdbc.query("""
                SELECT b.customer_gstin IS NOT NULL AS b2b,
                       (b.place_of_supply IS NULL OR b.place_of_supply = :state) AS intra,
                       sum(i.taxable_amount) AS amount
                """ + BILLED_LINES + " AND i.tax_rate = 0 GROUP BY 1, 2 ORDER BY 1, 2", params,
                (rs, n) -> new Gstr1Data.Nil(rs.getBoolean("b2b"), rs.getBoolean("intra"), rs.getBigDecimal("amount")));

        List<Gstr1Data.Hsn> hsn = jdbc.query("SELECT b.customer_gstin IS NOT NULL AS b2b, i.hsn_sac_code, "
                        + "i.unit_of_measure, i.tax_rate, sum(i.quantity) AS quantity, sum(i.total_amount) AS value, "
                        + TAX_SUMS + BILLED_LINES + " GROUP BY 1, i.hsn_sac_code, i.unit_of_measure, i.tax_rate"
                        + " ORDER BY 1 DESC, i.hsn_sac_code NULLS LAST, i.tax_rate", params,
                (rs, n) -> new Gstr1Data.Hsn(rs.getBoolean("b2b"), rs.getString("hsn_sac_code"),
                        rs.getString("unit_of_measure"), rs.getBigDecimal("tax_rate"), rs.getBigDecimal("quantity"),
                        rs.getBigDecimal("value"), rs.getBigDecimal("taxable"), rs.getBigDecimal("igst"),
                        rs.getBigDecimal("cgst"), rs.getBigDecimal("sgst"), rs.getBigDecimal("cess")));

        List<Gstr1Data.DocSeries> documents = jdbc.query("""
                SELECT split_part(bill_number, '/', 1) AS series, min(bill_number) AS first_number,
                       max(bill_number) AS last_number, count(*) AS total,
                       count(*) FILTER (WHERE status = 'CANCELLED') AS cancelled
                FROM pos.bills
                WHERE tenant_uuid = :tenant AND is_deleted = false AND bill_date >= :start AND bill_date < :end
                GROUP BY series ORDER BY series
                """, params, (rs, n) -> new Gstr1Data.DocSeries(rs.getString("series"), rs.getString("first_number"),
                rs.getString("last_number"), rs.getLong("total"), rs.getLong("cancelled")));

        return new Gstr1Data(gstin, state, month, b2b, b2clRows, b2cs, nil, hsn, documents);
    }

    // ---------------------------------------------------------------- helpers

    private static MapSqlParameterSource params(ReportPeriod period) {
        return new MapSqlParameterSource("tenant", TenantContext.requireTenantUuid())
                .addValue("start", period.start())
                .addValue("end", period.end());
    }

    private static BigDecimal totalTax(ResultSet rs) throws SQLException {
        return rs.getBigDecimal("cgst").add(rs.getBigDecimal("sgst")).add(rs.getBigDecimal("igst"))
                .add(rs.getBigDecimal("cess"));
    }

    private static BigDecimal share(BigDecimal part, BigDecimal total) {
        return total.signum() == 0 ? ZERO : part.multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal average(BigDecimal total, long count) {
        return count == 0 ? ZERO : total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return values.stream().reduce(ZERO, BigDecimal::add);
    }

    private static BigDecimal money(Object value) {
        return value == null ? ZERO : new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
    }

    private static long count(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
