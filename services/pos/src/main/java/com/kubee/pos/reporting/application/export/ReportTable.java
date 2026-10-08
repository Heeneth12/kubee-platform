package com.kubee.pos.reporting.application.export;

import java.util.List;

/**
 * One table of a report, ready to write as CSV or a spreadsheet sheet. Cell values are String, Number,
 * LocalDate / LocalDateTime or null.
 */
public record ReportTable(String title, List<String> headers, List<List<Object>> rows) {
}
