package com.kubee.pos.reporting.application.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Busy hours: completed orders per hour of day (0–23, all 24 rows), summed over the period. */
public record HourlySalesReport(LocalDate from, LocalDate to, List<Hour> hours) {

    public record Hour(int hour, long orders, BigDecimal netSales, BigDecimal averageOrderValue) {
    }
}
