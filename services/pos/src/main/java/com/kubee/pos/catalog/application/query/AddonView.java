package com.kubee.pos.catalog.application.query;

import com.kubee.pos.catalog.domain.FoodType;

import java.math.BigDecimal;

public record AddonView(
        String uuid,
        String name,
        BigDecimal price,
        FoodType foodType,
        int sortOrder,
        boolean active
) {
}
