package com.kubee.pos.billing.application.query;

import java.math.BigDecimal;
import java.util.List;

/** {@code unitPrice} includes add-ons. */
public record BillLineView(
        String uuid,
        String itemName,
        String addonsText,
        String hsnSacCode,
        String unitOfMeasure,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineAmount,
        BigDecimal discountAmount,
        BigDecimal taxRate,
        BigDecimal taxableAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        List<BillLineTaxView> taxes
) {
}
