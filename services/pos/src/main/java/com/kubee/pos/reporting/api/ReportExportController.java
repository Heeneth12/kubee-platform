package com.kubee.pos.reporting.api;

import com.kubee.pos.common.application.NotFoundException;
import com.kubee.pos.common.cqrs.QueryBus;
import com.kubee.pos.common.domain.DomainException;
import com.kubee.pos.reporting.application.export.ExportFormat;
import com.kubee.pos.reporting.application.export.ReportFileWriter;
import com.kubee.pos.reporting.application.export.ReportTable;
import com.kubee.pos.reporting.application.export.ReportTables;
import com.kubee.pos.reporting.application.query.CancellationsQuery;
import com.kubee.pos.reporting.application.query.CategorySalesQuery;
import com.kubee.pos.reporting.application.query.ChannelSalesQuery;
import com.kubee.pos.reporting.application.query.GstSummaryQuery;
import com.kubee.pos.reporting.application.query.Gstr1Query;
import com.kubee.pos.reporting.application.query.HourlySalesQuery;
import com.kubee.pos.reporting.application.query.ItemSalesQuery;
import com.kubee.pos.reporting.application.query.PaymentModesQuery;
import com.kubee.pos.reporting.application.query.ReportPeriod;
import com.kubee.pos.reporting.application.query.SalesSummaryQuery;
import com.kubee.pos.reporting.application.query.ShiftsQuery;
import com.kubee.pos.reporting.application.query.StaffSalesQuery;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/** File downloads of the reports (CSV / Excel) and the monthly GSTR-1 JSON. Manager-only like all reports. */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportExportController {

    private final QueryBus queryBus;
    private final Map<ExportFormat, ReportFileWriter> writers = new EnumMap<>(ExportFormat.class);
    private final Map<String, Function<ExportRequest, List<ReportTable>>> reports;

    private record ExportRequest(ReportPeriod period, String categoryUuid, ItemSalesQuery.Sort sort) {
    }

    public ReportExportController(QueryBus queryBus, List<ReportFileWriter> writers) {
        this.queryBus = queryBus;
        writers.forEach(w -> this.writers.put(w.format(), w));
        this.reports = Map.ofEntries(
                Map.entry("sales-summary", r -> ReportTables.of(queryBus.ask(new SalesSummaryQuery(r.period())))),
                Map.entry("payment-modes", r -> ReportTables.of(queryBus.ask(new PaymentModesQuery(r.period())))),
                Map.entry("channels", r -> ReportTables.of(queryBus.ask(new ChannelSalesQuery(r.period())))),
                Map.entry("items", r -> ReportTables.of(queryBus.ask(
                        new ItemSalesQuery(r.period(), r.categoryUuid(), r.sort())))),
                Map.entry("categories", r -> ReportTables.of(queryBus.ask(new CategorySalesQuery(r.period())))),
                Map.entry("hourly", r -> ReportTables.of(queryBus.ask(new HourlySalesQuery(r.period())))),
                Map.entry("staff", r -> ReportTables.of(queryBus.ask(new StaffSalesQuery(r.period())))),
                Map.entry("gst", r -> ReportTables.of(queryBus.ask(new GstSummaryQuery(r.period())))),
                Map.entry("cancellations", r -> ReportTables.of(queryBus.ask(new CancellationsQuery(r.period())))),
                Map.entry("shifts", r -> ReportTables.of(queryBus.ask(new ShiftsQuery(r.period())))));
    }

    /**
     * {@code GET /api/v1/reports/items/export?format=xlsx&from=2026-10-01&to=2026-10-31}. Same filters as the
     * report itself. CSV puts several tables one after another; Excel gives each its own sheet.
     */
    @GetMapping("/{report}/export")
    public ResponseEntity<byte[]> export(
            @PathVariable String report,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String categoryUuid,
            @RequestParam(required = false) ItemSalesQuery.Sort sort) {
        Function<ExportRequest, List<ReportTable>> tables = reports.get(report);
        if (tables == null) {
            throw new NotFoundException("No report called " + report + ". Use one of " + reports.keySet());
        }
        ExportFormat exportFormat = parseFormat(format);
        var period = new ReportPeriod(from, to);
        byte[] file = writers.get(exportFormat).write(tables.apply(new ExportRequest(period, categoryUuid, sort)));
        String name = report + "_" + period.from() + "_" + period.to() + "." + exportFormat.extension();
        return download(file, MediaType.parseMediaType(exportFormat.contentType()), name);
    }

    /** GSTR-1 JSON for one month, for the GST offline tool: {@code GET /api/v1/reports/gstr1?month=2026-10}. */
    @GetMapping("/gstr1")
    public ResponseEntity<Map<String, Object>> gstr1(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        Map<String, Object> json = queryBus.ask(new Gstr1Query(month));
        String name = "GSTR1_" + json.get("gstin") + "_" + json.get("fp") + ".json";
        return download(json, MediaType.APPLICATION_JSON, name);
    }

    private static ExportFormat parseFormat(String format) {
        try {
            return ExportFormat.valueOf(format.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new DomainException("Format must be csv or xlsx");
        }
    }

    private static <T> ResponseEntity<T> download(T body, MediaType type, String fileName) {
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName).build().toString())
                .body(body);
    }
}
