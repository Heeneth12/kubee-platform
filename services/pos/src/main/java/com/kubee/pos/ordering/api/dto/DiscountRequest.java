package com.kubee.pos.ordering.api.dto;

import com.kubee.pos.ordering.domain.Discount;
import com.kubee.pos.ordering.domain.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Bill-level discount: {@code {"type":"PERCENT","value":10}} or {@code {"type":"FLAT","value":50}}. */
public record DiscountRequest(
        @NotNull DiscountType type,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 16, fraction = 2) BigDecimal value,
        @Size(max = 255) String reason
) {

    public Discount toDiscount() {
        return new Discount(type, value);
    }
}
