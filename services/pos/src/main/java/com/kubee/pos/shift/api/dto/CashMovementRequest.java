package com.kubee.pos.shift.api.dto;

import com.kubee.pos.shift.domain.CashMovementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** {@code {"type":"OUT","amount":500,"reason":"Milk supplier"}} */
public record CashMovementRequest(
        @NotNull CashMovementType type,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 16, fraction = 2) BigDecimal amount,
        @NotBlank @Size(max = 255) String reason
) {
}
