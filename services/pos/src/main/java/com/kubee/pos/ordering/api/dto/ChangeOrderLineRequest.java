package com.kubee.pos.ordering.api.dto;

import com.kubee.pos.ordering.domain.Discount;
import com.kubee.pos.ordering.domain.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Full replace of the editable parts of a line: leaving out the discount or note removes it. */
public record ChangeOrderLineRequest(
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 9, fraction = 3) BigDecimal quantity,
        DiscountType discountType,
        @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal discountValue,
        @Size(max = 255) String notes
) {

    public Discount discount() {
        return Discount.ofNullable(discountType, discountValue);
    }
}
