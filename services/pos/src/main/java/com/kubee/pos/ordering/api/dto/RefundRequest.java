package com.kubee.pos.ordering.api.dto;

import com.kubee.pos.ordering.domain.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** {@code method} defaults to the original payment's method. */
public record RefundRequest(
        @Size(max = 64) String clientRef,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 16, fraction = 2) BigDecimal amount,
        PaymentMethod method,
        @NotBlank @Size(max = 255) String reason
) {
}
