package com.kubee.pos.reporting.application.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Per staff member (auth service user uuid; the app shows names from its user list): orders they took that were
 * completed in the period, money they received and refunded, and what they cancelled.
 */
public record StaffSalesReport(LocalDate from, LocalDate to, List<Row> staff) {

    public record Row(
            String userUuid,
            long completedOrders,
            BigDecimal netSales,
            BigDecimal discountAmount,
            long paymentCount,
            BigDecimal paymentAmount,
            long refundCount,
            BigDecimal refundAmount,
            long cancelledOrders,
            long cancelledBills
    ) {
    }
}
