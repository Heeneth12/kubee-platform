package com.kubee.pos.reporting.application.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Report 2: money in and out per payment method (successful payments by payment time). */
public record PaymentModeReport(LocalDate from, LocalDate to, List<Row> methods, Row total) {

    /** @param method CASH, UPI, CARD ... ; null on the total row */
    public record Row(String method, long paymentCount, BigDecimal paymentAmount, long refundCount,
                      BigDecimal refundAmount, BigDecimal netAmount) {
    }
}
