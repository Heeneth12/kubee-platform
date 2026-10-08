package com.kubee.pos.reporting.api;

import com.kubee.pos.common.cqrs.QueryBus;
import com.kubee.pos.common.web.ApiResponse;
import com.kubee.pos.reporting.application.query.CancellationReport;
import com.kubee.pos.reporting.application.query.CancellationsQuery;
import com.kubee.pos.reporting.application.query.ChannelSalesQuery;
import com.kubee.pos.reporting.application.query.ChannelSalesReport;
import com.kubee.pos.reporting.application.query.GstReport;
import com.kubee.pos.reporting.application.query.StaffSalesReport;
import com.kubee.pos.reporting.application.query.StaffSalesQuery;
import com.kubee.pos.reporting.application.query.ShiftsQuery;
import com.kubee.pos.reporting.application.query.ShiftReport;
import com.kubee.pos.reporting.application.query.HourlySalesReport;
import com.kubee.pos.reporting.application.query.HourlySalesQuery;
import com.kubee.pos.reporting.application.query.CategorySalesReport;
import com.kubee.pos.reporting.application.query.CategorySalesQuery;
import com.kubee.pos.reporting.application.query.GstSummaryQuery;
import com.kubee.pos.reporting.application.query.ItemSalesQuery;
import com.kubee.pos.reporting.application.query.ItemSalesReport;
import com.kubee.pos.reporting.application.query.PaymentModeReport;
import com.kubee.pos.reporting.application.query.PaymentModesQuery;
import com.kubee.pos.reporting.application.query.ReportPeriod;
import com.kubee.pos.reporting.application.query.SalesSummaryQuery;
import com.kubee.pos.reporting.application.query.SalesSummaryReport;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Sales, payment-mode, channel, item, category, hourly, staff, GST, cancellation and shift reports. Every report takes {@code from} / {@code to} (yyyy-MM-dd, inclusive); both default
 * to today, so {@code GET /api/v1/reports/sales-summary} is today's day-end summary.
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportQueryController {

    private final QueryBus queryBus;

    @GetMapping("/sales-summary")
    public ResponseEntity<ApiResponse<SalesSummaryReport>> salesSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(queryBus.ask(new SalesSummaryQuery(new ReportPeriod(from, to))));
    }

    @GetMapping("/payment-modes")
    public ResponseEntity<ApiResponse<PaymentModeReport>> paymentModes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(queryBus.ask(new PaymentModesQuery(new ReportPeriod(from, to))));
    }

    /** Counter vs Zomato vs Swiggy: completed and cancelled orders per order source. */
    @GetMapping("/channels")
    public ResponseEntity<ApiResponse<ChannelSalesReport>> channels(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(queryBus.ask(new ChannelSalesQuery(new ReportPeriod(from, to))));
    }

    @GetMapping("/items")
    public ResponseEntity<ApiResponse<ItemSalesReport>> items(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String categoryUuid,
            @RequestParam(required = false) ItemSalesQuery.Sort sort) {
        return ApiResponse.ok(queryBus.ask(new ItemSalesQuery(new ReportPeriod(from, to), categoryUuid, sort)));
    }

    @GetMapping("/gst")
    public ResponseEntity<ApiResponse<GstReport>> gst(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(queryBus.ask(new GstSummaryQuery(new ReportPeriod(from, to))));
    }

    /** Busy hours: all 24 hours of the day, summed over the period. */
    @GetMapping("/hourly")
    public ResponseEntity<ApiResponse<HourlySalesReport>> hourly(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(queryBus.ask(new HourlySalesQuery(new ReportPeriod(from, to))));
    }

    @GetMapping("/staff")
    public ResponseEntity<ApiResponse<StaffSalesReport>> staff(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(queryBus.ask(new StaffSalesQuery(new ReportPeriod(from, to))));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<CategorySalesReport>> categories(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(queryBus.ask(new CategorySalesQuery(new ReportPeriod(from, to))));
    }

    /** Cash-drawer history: shifts opened in the period, with expected vs counted cash. */
    @GetMapping("/shifts")
    public ResponseEntity<ApiResponse<ShiftReport>> shifts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(queryBus.ask(new ShiftsQuery(new ReportPeriod(from, to))));
    }

    @GetMapping("/cancellations")
    public ResponseEntity<ApiResponse<CancellationReport>> cancellations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(queryBus.ask(new CancellationsQuery(new ReportPeriod(from, to))));
    }
}
