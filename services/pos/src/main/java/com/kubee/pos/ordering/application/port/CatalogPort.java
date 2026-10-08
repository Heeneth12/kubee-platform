package com.kubee.pos.ordering.application.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** What ordering needs from the catalog: everything required to put an item on an order. */
public interface CatalogPort {

    /** The item with its variants and linked add-on groups, or empty if it does not exist (or is deleted). */
    Optional<SellableItem> findItem(String itemUuid);

    record SellableItem(
            long id,
            String uuid,
            String name,
            String hsnSacCode,
            String unitOfMeasure,
            BigDecimal sellingPrice,
            boolean priceIncludesTax,
            Long taxGroupId,
            BigDecimal taxRate,
            boolean hasVariants,
            boolean openPrice,
            boolean active,
            List<SellableVariant> variants,
            List<SellableAddonGroup> addonGroups
    ) {
    }

    record SellableVariant(long id, String uuid, String name, BigDecimal sellingPrice, boolean active) {
    }

    /** @param maxSelect null = no limit */
    record SellableAddonGroup(String uuid, String name, int minSelect, Integer maxSelect, boolean active,
                              List<SellableAddon> addons) {
    }

    record SellableAddon(long id, String uuid, String name, BigDecimal price, boolean active) {
    }
}
