package com.kubee.pos.ordering.api.dto;

import com.kubee.pos.ordering.application.command.LineInput;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/** {@code quantity} per unit of the line, default 1. */
public record AddonChoiceRequest(
        @NotBlank String addonUuid,
        @DecimalMin(value = "0", inclusive = false) @Digits(integer = 9, fraction = 3) BigDecimal quantity
) {

    LineInput.AddonChoice toChoice() {
        return new LineInput.AddonChoice(addonUuid, quantity);
    }
}
