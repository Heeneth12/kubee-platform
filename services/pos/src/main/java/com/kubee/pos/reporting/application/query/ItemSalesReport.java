package com.kubee.pos.reporting.application.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Report 3: what sold, per item and variant, from orders COMPLETED in the period. Amounts are line amounts
 * (after discounts, incl. GST) and do not include the order round-off. Refunds are not item-wise.
 */
public record ItemSalesReport(LocalDate from, LocalDate to, BigDecimal totalNetAmount, List<Row> items) {

    /** @param shareOfSales % of {@code totalNetAmount} */
    public record Row(
            String itemUuid,
            String itemName,
            String variantUuid,
            String variantName,
            String categoryName,
            String unitOfMeasure,
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
