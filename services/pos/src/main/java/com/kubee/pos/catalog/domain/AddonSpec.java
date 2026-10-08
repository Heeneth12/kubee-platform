package com.kubee.pos.catalog.domain;

import com.kubee.pos.common.domain.Money;

/** Desired state of one add-on, same upsert rules as {@link VariantSpec}. */
public record AddonSpec(
        String uuid,
        String name,
        Money price,
        FoodType foodType,
        int sortOrder,
        boolean active
) {
}
