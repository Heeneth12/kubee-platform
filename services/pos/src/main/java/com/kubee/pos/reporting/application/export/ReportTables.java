package com.kubee.pos.reporting.application.export;

import com.kubee.pos.reporting.application.query.CancellationReport;
import com.kubee.pos.reporting.application.query.CategorySalesReport;
import com.kubee.pos.reporting.application.query.ChannelSalesReport;
import com.kubee.pos.reporting.application.query.GstReport;
import com.kubee.pos.reporting.application.query.HourlySalesReport;
import com.kubee.pos.reporting.application.query.ItemSalesReport;
import com.kubee.pos.reporting.application.query.PaymentModeReport;
import com.kubee.pos.reporting.application.query.SalesSummaryReport;
import com.kubee.pos.reporting.application.query.ShiftReport;
import com.kubee.pos.reporting.application.query.StaffSalesReport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/** Lays each report out as one or more plain tables (one CSV section / one spreadsheet sheet each). */
public final class ReportTables {

    private ReportTables() {
    }

    public static List<ReportTable> of(SalesSummaryReport r) {
        var summary = new ReportTable("Summary", List.of("Metric", "Value"), List.of(
                row("From", r.from()), row("To", r.to()),
                row("Completed orders", r.completedOrders()), row("Gross sales", r.grossSales()),
                row("Discounts", r.discountAmount()), row("Taxable amount", r.taxableAmount()),
                row("Tax", r.taxAmount()), row("Round-off", r.roundOffAmount()), row("Net sales", r.netSales()),
                row("Average order value", r.averageOrderValue()), row("Collected", r.collected()),
                row("Refunded", r.refunded()), row("Net collected", r.netCollected()),
                row("Cancelled orders", r.cancelledOrders()), row("Bills issued", r.billsIssued()),
                row("Bills cancelled", r.billsCancelled())));
        var days = table("Daily", List.of("Date", "Orders", "Net sales", "Tax", "Average order value"), r.days(),
                d -> row(d.date(), d.orders(), d.netSales(), d.taxAmount(), d.averageOrderValue()));
        return List.of(summary, days);
    }

    public static List<ReportTable> of(PaymentModeReport r) {
        List<List<Object>> rows = new ArrayList<>(r.methods().stream().map(ReportTables::paymentRow).toList());
        rows.add(paymentRow(r.total()));
        return List.of(new ReportTable("Payment modes", List.of("Method", "Payments", "Amount received", "Refunds",
                "Amount refunded", "Net"), rows));
    }

    public static List<ReportTable> of(ItemSalesReport r) {
        return List.of(table("Item sales", List.of("Item", "Variant", "Category", "Unit", "Quantity", "Orders",
                        "Gross", "Discount", "Taxable", "Tax", "Net", "Share %"), r.items(),
                i -> row(i.itemName(), i.variantName(), i.categoryName(), i.unitOfMeasure(), i.quantity(),
                        i.orderCount(), i.grossAmount(), i.discountAmount(), i.taxableAmount(), i.taxAmount(),
                        i.netAmount(), i.shareOfSales())));
    }

    public static List<ReportTable> of(CategorySalesReport r) {
        return List.of(table("Category sales", List.of("Category", "Parent", "Quantity", "Orders", "Gross",
                        "Discount", "Taxable", "Tax", "Net", "Share %"), r.categories(),
                c -> row(c.categoryName(), c.parentCategoryName(), c.quantity(), c.orderCount(), c.grossAmount(),
                        c.discountAmount(), c.taxableAmount(), c.taxAmount(), c.netAmount(), c.shareOfSales())));
    }

    public static List<ReportTable> of(ChannelSalesReport r) {
        List<ChannelSalesReport.Row> rows = new ArrayList<>(r.channels());
        rows.add(r.total());
        return List.of(table("Channel sales", List.of("Channel", "Completed orders", "Gross", "Discount", "Tax",
                        "Net sales", "Average order value", "Share %", "Cancelled orders"), rows,
                c -> row(c.source() == null ? "Total" : c.source(), c.completedOrders(), c.grossSales(),
                        c.discountAmount(), c.taxAmount(), c.netSales(), c.averageOrderValue(), c.shareOfSales(),
                        c.cancelledOrders())));
    }

    public static List<ReportTable> of(HourlySalesReport r) {
        return List.of(table("Hourly sales", List.of("Hour", "Orders", "Net sales", "Average order value"),
                r.hours(), h -> row(String.format("%02d:00-%02d:59", h.hour(), h.hour()), h.orders(), h.netSales(),
                        h.averageOrderValue())));
    }

    public static List<ReportTable> of(StaffSalesReport r) {
        return List.of(table("Staff", List.of("User", "Completed orders", "Net sales", "Discounts", "Payments",
                        "Amount received", "Refunds", "Amount refunded", "Cancelled orders", "Cancelled bills"),
                r.staff(), s -> row(s.userUuid(), s.completedOrders(), s.netSales(), s.discountAmount(),
                        s.paymentCount(), s.paymentAmount(), s.refundCount(), s.refundAmount(), s.cancelledOrders(),
                        s.cancelledBills())));
    }

    public static List<ReportTable> of(GstReport r) {
        GstReport.Totals t = r.totals();
        var totals = new ReportTable("GST totals", List.of("Metric", "Value"), List.of(
                row("From", r.from()), row("To", r.to()), row("Invoices", t.invoiceCount()),
                row("Taxable amount", t.taxableAmount()), row("CGST", t.cgst()), row("SGST", t.sgst()),
                row("IGST", t.igst()), row("Cess", t.cess()), row("Total tax", t.totalTax()),
                row("Round-off", t.roundOff()), row("Invoice value", t.invoiceValue()),
                row("Completed orders without a bill", r.ordersWithoutBill()),
                row("Cancelled bills (excluded)", r.cancelledBills())));
        var byRate = table("By rate", List.of("Rate %", "Taxable", "CGST", "SGST", "IGST", "Cess", "Total tax",
                "Value"), r.byRate(), x -> row(x.rate(), x.taxableAmount(), x.cgst(), x.sgst(), x.igst(), x.cess(),
                x.totalTax(), x.invoiceValue()));
        var b2b = table("B2B invoices", List.of("Bill number", "Date", "Customer GSTIN", "Customer",
                "Place of supply", "Taxable", "CGST", "SGST", "IGST", "Cess", "Invoice value"), r.b2bInvoices(),
                x -> row(x.billNumber(), x.billDate(), x.customerGstin(), x.customerName(), x.placeOfSupply(),
                        x.taxableAmount(), x.cgst(), x.sgst(), x.igst(), x.cess(), x.invoiceValue()));
        var b2c = table("B2C by state and rate", List.of("Place of supply", "Rate %", "Taxable", "CGST", "SGST",
                "IGST", "Cess"), r.b2c(), x -> row(x.placeOfSupply(), x.rate(), x.taxableAmount(), x.cgst(), x.sgst(),
                x.igst(), x.cess()));
        var hsn = table("HSN summary", List.of("HSN/SAC", "Unit", "Rate %", "Quantity", "Taxable", "CGST", "SGST",
                "IGST", "Cess", "Total value"), r.hsn(), x -> row(x.hsnSacCode(), x.unitOfMeasure(), x.rate(),
                x.quantity(), x.taxableAmount(), x.cgst(), x.sgst(), x.igst(), x.cess(), x.totalValue()));
        return List.of(totals, byRate, b2b, b2c, hsn);
    }

    public static List<ReportTable> of(CancellationReport r) {
        var orders = table("Cancelled orders", List.of("Order", "Created", "Cancelled", "Cancelled by", "Reason",
                "Lines", "Value"), r.cancelledOrders(), o -> row(o.orderNumber(), o.createdAt(), o.cancelledAt(),
                o.cancelledBy(), o.reason(), o.lineCount(), o.orderValue()));
        var bills = table("Cancelled bills", List.of("Bill number", "Order", "Bill date", "Cancelled", "Cancelled by",
                "Reason", "Value"), r.cancelledBills(), b -> row(b.billNumber(), b.orderNumber(), b.billDate(),
                b.cancelledAt(), b.cancelledBy(), b.reason(), b.billValue()));
        var refunds = table("Refunds", List.of("Order", "Method", "Amount", "Reason", "Refunded at", "Refunded by"),
                r.refunds(), x -> row(x.orderNumber(), x.method(), x.amount(), x.reason(), x.refundedAt(),
                        x.refundedBy()));
        return List.of(orders, bills, refunds);
    }

    public static List<ReportTable> of(ShiftReport r) {
        return List.of(table("Shifts", List.of("Status", "Opened", "Opened by", "Closed", "Closed by",
                        "Opening cash", "Cash sales", "Cash in", "Cash out", "Expected", "Counted", "Difference", "Notes"),
                r.shifts(), s -> row(s.status(), s.openedAt(), s.openedBy(), s.closedAt(), s.closedBy(),
                        s.openingCash(), s.cashSales(), s.cashIn(), s.cashOut(), s.expectedCash(), s.countedCash(),
                        s.cashDifference(), s.closingNotes())));
    }

    // ------------------------------------------------------------------

    private static List<Object> paymentRow(PaymentModeReport.Row p) {
        return row(p.method() == null ? "TOTAL" : p.method(), p.paymentCount(), p.paymentAmount(), p.refundCount(),
                p.refundAmount(), p.netAmount());
    }

    private static <T> ReportTable table(String title, List<String> headers, List<T> items,
                                         Function<T, List<Object>> toRow) {
        return new ReportTable(title, headers, items.stream().map(toRow).toList());
    }

    /** Allows nulls (List.of does not). */
    private static List<Object> row(Object... cells) {
        return Arrays.asList(cells);
    }
}
