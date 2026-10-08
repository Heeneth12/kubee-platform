package com.kubee.pos.catalog.application.command;

import com.kubee.pos.catalog.domain.FoodType;
import com.kubee.pos.catalog.domain.ItemType;
import com.kubee.pos.catalog.domain.VariantSpec;
import com.kubee.pos.common.domain.Money;

import java.util.List;

/** Full desired state of an item, with other aggregates referenced by uuid. */
public record ItemInput(
        String name,
        String shortName,
        String itemCode,
        String barcode,
        String categoryUuid,
        ItemType itemType,
        FoodType foodType,
        String unitOfMeasure,
        String hsnSacCode,
        String imageUrl,
        String description,
        int sortOrder,
        Money sellingPrice,
        Money mrp,
        boolean priceIncludesTax,
        String taxGroupUuid,
        boolean openPrice,
        boolean favourite,
        boolean active,
        List<VariantSpec> variants,
        List<String> addonGroupUuids
) {
}
