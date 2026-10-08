package com.kubee.pos.reporting.infrastructure.export;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Text form of a cell: Indian date format, plain numbers, and no spreadsheet formulas from user input. */
final class CellText {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private CellText() {
    }

    static String of(Object value) {
        return switch (value) {
            case null -> "";
            case LocalDateTime t -> t.format(DATE_TIME);
            case LocalDate d -> d.format(DATE);
            case BigDecimal b -> b.toPlainString();
            case Number n -> n.toString();
            default -> safeText(value.toString());
        };
    }

    /** Customer names, reasons etc. are typed by users: never let Excel run them as formulas. */
    static String safeText(String text) {
        return !text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0 ? "'" + text : text;
    }
}
