package com.kubee.pos.billing.domain;

import com.kubee.pos.common.domain.Money;

import java.math.BigDecimal;
import java.util.List;

/**
 * One order line, already priced by the order. {@code taxComponents} are the line's tax group parts
 * (may be empty or out of date; the bill then falls back to an even CGST/SGST or a single IGST split).
 *
 * @param unitPrice including add-ons
 */
public record BillLineSpec(
        Long orderItemId,
        Long itemId,
        String itemName,
        String addonsText,
        String hsnSacCode,
        String unitOfMeasure,
        BigDecimal quantity,
        Money unitPrice,
        Money lineAmount,
        Money discountAmount,
        BigDecimal taxRate,
        Money taxableAmount,
        Money taxAmount,
        Money totalAmount,
        List<TaxComponentSpec> taxComponents
) {

    public BillLineSpec {
        taxComponents = taxComponents == null ? List.of() : List.copyOf(taxComponents);
    }
}
