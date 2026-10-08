package com.kubee.pos.catalog.domain;

/** Descriptive fields of an item (everything except price, variants and add-ons). */
public record ItemDetails(
        String name,
        String shortName,
        String itemCode,
        String barcode,
        Long categoryId,
        ItemType itemType,
        FoodType foodType,
        String unitOfMeasure,
        String hsnSacCode,
        String imageUrl,
        String description,
        int sortOrder
) {
}
