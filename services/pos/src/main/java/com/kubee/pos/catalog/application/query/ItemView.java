package com.kubee.pos.catalog.application.query;

import com.kubee.pos.catalog.domain.FoodType;
import com.kubee.pos.catalog.domain.ItemType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Everything the billing screen needs about one item. Add-on groups are referenced by uuid. */
public record ItemView(
        String uuid,
        String name,
        String shortName,
        String itemCode,
        String barcode,
        String categoryUuid,
        String categoryName,
        ItemType itemType,
        FoodType foodType,
        String unitOfMeasure,
        BigDecimal sellingPrice,
        BigDecimal mrp,
        boolean priceIncludesTax,
        String taxGroupUuid,
        String taxGroupName,
        BigDecimal taxRate,
        String hsnSacCode,
        boolean hasVariants,
        boolean openPrice,
        boolean favourite,
        String imageUrl,
        String description,
        int sortOrder,
        boolean active,
        LocalDateTime updatedAt,
        List<VariantView> variants,
        List<String> addonGroupUuids
) {
}
