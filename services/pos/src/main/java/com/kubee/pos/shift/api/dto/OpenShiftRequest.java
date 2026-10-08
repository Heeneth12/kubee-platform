package com.kubee.pos.shift.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OpenShiftRequest(
        @NotNull @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal openingCash,
        String notes
) {
}
