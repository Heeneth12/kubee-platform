package com.kubee.pos.ordering.domain;

import com.kubee.pos.common.domain.Money;

import java.math.BigDecimal;
import java.util.List;

/**
 * Everything an order line needs, already resolved from the catalog. Name, price and tax are
 * snapshots: later catalog changes never alter an existing order.
 *
 * @param taxRate total GST % of the item's tax group (0 when untaxed)
 */
public record LineSpec(
        Long itemId,
        Long variantId,
        String itemName,
        String variantName,
        String hsnSacCode,
        String unitOfMeasure,
        BigDecimal quantity,
        Money unitPrice,
        boolean priceIncludesTax,
        Long taxGroupId,
        BigDecimal taxRate,
        List<LineAddonSpec> addons,
        Discount discount,
        String notes
) {

    public LineSpec {
        addons = addons == null ? List.of() : List.copyOf(addons);
        taxRate = taxRate == null ? BigDecimal.ZERO : taxRate;
    }
}
