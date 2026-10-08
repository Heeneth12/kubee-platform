package com.kubee.pos.reporting.application.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Report 1: day sales. Sales = orders COMPLETED in the period (by completion time).
 * {@code collected} / {@code refunded} are money movements in the period (by payment time), so they can
 * include payments for orders completed on another day.
 */
public record SalesSummaryReport(
        LocalDate from,
        LocalDate to,
        long completedOrders,
        BigDecimal grossSales,
        BigDecimal discountAmount,
        BigDecimal taxableAmount,
        BigDecimal taxAmount,
        BigDecimal roundOffAmount,
        BigDecimal netSales,
        BigDecimal averageOrderValue,
        BigDecimal collected,
        BigDecimal refunded,
        BigDecimal netCollected,
        long cancelledOrders,
        long billsIssued,
        long billsCancelled,
        List<Day> days
) {

    public record Day(LocalDate date, long orders, BigDecimal netSales, BigDecimal taxAmount,
                      BigDecimal averageOrderValue) {
    }
}
