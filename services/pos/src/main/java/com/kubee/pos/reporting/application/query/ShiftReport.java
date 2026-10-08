package com.kubee.pos.reporting.application.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cash-drawer history: shifts opened in the period with their tally. Open shifts show live figures and no count.
 * {@code totalShort} is the sum of negative differences (as a positive number), {@code totalExcess} of positive ones.
 */
public record ShiftReport(LocalDate from, LocalDate to, long shiftCount, BigDecimal totalShort, BigDecimal totalExcess,
                          List<Row> shifts) {

    public record Row(
            String shiftUuid,
            String status,
            LocalDateTime openedAt,
            String openedBy,
            LocalDateTime closedAt,
            String closedBy,
            BigDecimal openingCash,
            BigDecimal cashSales,
            BigDecimal cashIn,
            BigDecimal cashOut,
            BigDecimal expectedCash,
            BigDecimal countedCash,
            BigDecimal cashDifference,
            String closingNotes
    ) {
    }
}
