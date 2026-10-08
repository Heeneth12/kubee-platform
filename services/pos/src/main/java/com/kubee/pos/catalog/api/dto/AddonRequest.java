package com.kubee.pos.catalog.api.dto;

import com.kubee.pos.catalog.domain.AddonSpec;
import com.kubee.pos.catalog.domain.FoodType;
import com.kubee.pos.common.domain.Money;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Same upsert rules as {@link VariantRequest}. Missing price means free. */
public record AddonRequest(
        String uuid,
        @NotBlank @Size(max = 100) String name,
        @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal price,
        FoodType foodType,
        Integer sortOrder,
        Boolean active
) {

    public AddonSpec toSpec() {
        return new AddonSpec(uuid, name, Money.ofNullable(price), foodType, sortOrder == null ? 0 : sortOrder,
                active == null || active);
    }
}
