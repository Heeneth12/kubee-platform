package com.kubee.pos.shift.application.query;

import com.kubee.pos.shift.domain.CashMovementType;
import com.kubee.pos.shift.domain.ShiftStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A shift with its cash tally. For an OPEN shift {@code cashSales}, {@code cashIn}, {@code cashOut} and
 * {@code expectedCash} are live (up to now) and {@code countedCash} / {@code cashDifference} are null.
 */
public record ShiftView(
        String uuid,
        ShiftStatus status,
        String openedBy,
        LocalDateTime openedAt,
        BigDecimal openingCash,
        String openingNotes,
        String closedBy,
        LocalDateTime closedAt,
        String closingNotes,
        BigDecimal cashSales,
        BigDecimal cashIn,
        BigDecimal cashOut,
        BigDecimal expectedCash,
        BigDecimal countedCash,
        BigDecimal cashDifference,
        long completedOrders,
        BigDecimal netSales,
        List<PaymentTotal> payments,
        List<Movement> movements
) {

    /** Money in/out per method during the shift. */
    public record PaymentTotal(String method, long paymentCount, BigDecimal paymentAmount, long refundCount,
                               BigDecimal refundAmount, BigDecimal netAmount) {
    }

    public record Movement(String uuid, CashMovementType type, BigDecimal amount, String reason, String createdBy,
                           LocalDateTime movedAt) {
    }
}
