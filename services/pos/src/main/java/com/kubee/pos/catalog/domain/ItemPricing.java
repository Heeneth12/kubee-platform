package com.kubee.pos.catalog.domain;

import com.kubee.pos.common.domain.Money;

/**
 * @param sellingPrice ignored when the item has variants (the default variant's price is used);
 *                     may be null for open-price items (treated as 0)
 * @param taxGroupId   null means the item is not taxed
 */
public record ItemPricing(
        Money sellingPrice,
        Money mrp,
        boolean priceIncludesTax,
        Long taxGroupId,
        boolean openPrice
) {
}
