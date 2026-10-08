package com.kubee.pos.reporting.application.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Item-wise sales rolled up to each item's current category. {@code categoryUuid} null = uncategorised. */
public record CategorySalesReport(LocalDate from, LocalDate to, BigDecimal totalNetAmount, List<Row> categories) {

    public record Row(
            String categoryUuid,
            String categoryName,
            String parentCategoryName,
            BigDecimal quantity,
            long orderCount,
            BigDecimal grossAmount,
            BigDecimal discountAmount,
            BigDecimal taxableAmount,
            BigDecimal taxAmount,
            BigDecimal netAmount,
            BigDecimal shareOfSales
    ) {
    }
}
