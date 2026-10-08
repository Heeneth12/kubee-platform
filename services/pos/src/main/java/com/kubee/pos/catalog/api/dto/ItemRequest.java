package com.kubee.pos.catalog.api.dto;

import com.kubee.pos.catalog.application.command.ItemInput;
import com.kubee.pos.catalog.domain.FoodType;
import com.kubee.pos.catalog.domain.ItemType;
import com.kubee.pos.common.domain.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** Used for both create (POST) and full update (PUT). Booleans left out take their defaults. */
public record ItemRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 50) String shortName,
        @Size(max = 50) String itemCode,
        @Size(max = 100) String barcode,
        String categoryUuid,
        ItemType itemType,
        FoodType foodType,
        @Size(max = 20) String unitOfMeasure,
        @Size(max = 20) String hsnSacCode,
        @Size(max = 500) String imageUrl,
        String description,
        Integer sortOrder,
        @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal sellingPrice,
        @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal mrp,
        Boolean priceIncludesTax,
        String taxGroupUuid,
        Boolean openPrice,
        Boolean favourite,
        Boolean active,
        @Valid List<VariantRequest> variants,
        List<String> addonGroupUuids
) {

    public ItemInput toInput() {
        return new ItemInput(name, shortName, itemCode, barcode, categoryUuid, itemType, foodType, unitOfMeasure,
                hsnSacCode, imageUrl, description, sortOrder == null ? 0 : sortOrder,
                Money.ofNullable(sellingPrice), Money.ofNullable(mrp),
                priceIncludesTax == null || priceIncludesTax, taxGroupUuid,
                Boolean.TRUE.equals(openPrice), Boolean.TRUE.equals(favourite), active == null || active,
                variants == null ? List.of() : variants.stream().map(VariantRequest::toSpec).toList(),
                addonGroupUuids == null ? List.of() : addonGroupUuids);
    }
}
