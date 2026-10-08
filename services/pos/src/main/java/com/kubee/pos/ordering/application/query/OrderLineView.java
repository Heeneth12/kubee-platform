package com.kubee.pos.ordering.application.query;

import com.kubee.pos.ordering.domain.DiscountType;

import java.math.BigDecimal;
import java.util.List;

/** {@code itemUuid} / {@code variantUuid} are null if the catalog record was deleted since. */
public record OrderLineView(
        String uuid,
        String itemUuid,
        String variantUuid,
        String itemName,
        String variantName,
        String hsnSacCode,
        String unitOfMeasure,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal addonsUnitPrice,
        boolean priceIncludesTax,
        BigDecimal lineAmount,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal discountAmount,
        BigDecimal taxRate,
        BigDecimal taxableAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String notes,
        List<OrderLineAddonView> addons
) {
}
