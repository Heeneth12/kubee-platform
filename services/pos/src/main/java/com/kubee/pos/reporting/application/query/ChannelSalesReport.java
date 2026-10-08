package com.kubee.pos.reporting.application.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Sales per order source (POS counter, Zomato, Swiggy, other online): orders completed in the period and
 * orders cancelled in the period. Only sources with activity are listed.
 */
public record ChannelSalesReport(LocalDate from, LocalDate to, List<Row> channels, Row total) {

    /** @param source POS, ZOMATO, SWIGGY, OTHER; null on the total row */
    public record Row(
            String source,
            long completedOrders,
            BigDecimal grossSales,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            BigDecimal netSales,
            BigDecimal averageOrderValue,
            BigDecimal shareOfSales,
            long cancelledOrders
    ) {
    }
}
