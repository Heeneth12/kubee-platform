package com.kubee.pos.ordering.application.command;

import com.kubee.pos.ordering.domain.Discount;

import java.math.BigDecimal;
import java.util.List;

/**
 * A line as the billing screen sends it: catalog references plus what the cashier chose.
 *
 * @param variantUuid required when the item has variants
 * @param unitPrice   only for open-price items (the cashier types the price)
 */
public record LineInput(
        String itemUuid,
        String variantUuid,
        BigDecimal quantity,
        BigDecimal unitPrice,
        List<AddonChoice> addons,
        Discount discount,
        String notes
) {

    public LineInput {
        addons = addons == null ? List.of() : List.copyOf(addons);
        quantity = quantity == null ? BigDecimal.ONE : quantity;
    }

    /** @param quantity per unit of the line; 1 when not given */
    public record AddonChoice(String addonUuid, BigDecimal quantity) {

        public AddonChoice {
            quantity = quantity == null ? BigDecimal.ONE : quantity;
        }
    }
}
