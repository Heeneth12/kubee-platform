package com.kubee.pos.ordering.api.dto;

import com.kubee.pos.ordering.application.command.LineInput;
import com.kubee.pos.ordering.domain.Discount;
import com.kubee.pos.ordering.domain.DiscountType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Add one item to an order. {@code quantity} defaults to 1; {@code variantUuid} is required for items
 * with variants; {@code unitPrice} only for open-price items.
 */
public record OrderLineRequest(
        @NotBlank String itemUuid,
        String variantUuid,
        @DecimalMin(value = "0", inclusive = false) @Digits(integer = 9, fraction = 3) BigDecimal quantity,
        @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal unitPrice,
        @Valid List<AddonChoiceRequest> addons,
        DiscountType discountType,
        @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal discountValue,
        @Size(max = 255) String notes
) {

    public LineInput toInput() {
        return new LineInput(itemUuid, variantUuid, quantity, unitPrice,
                addons == null ? List.of() : addons.stream().map(AddonChoiceRequest::toChoice).toList(),
                Discount.ofNullable(discountType, discountValue), notes);
    }
}
