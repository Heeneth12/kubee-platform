package com.kubee.pos.ordering.api.dto;

import com.kubee.pos.common.domain.Money;
import com.kubee.pos.ordering.domain.PaymentMethod;
import com.kubee.pos.ordering.domain.PaymentSpec;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Take a payment. CASH: send {@code tenderedAmount} (what the customer gave) and optionally {@code amount};
 * without {@code amount} the order's due (or the tendered amount, if less) is used and the change is worked out.
 * UPI / CARD / others: send {@code amount}, and {@code referenceNo} for the UTR / approval code.
 */
public record PaymentRequest(
        @Size(max = 64) String clientRef,
        @NotNull PaymentMethod method,
        @DecimalMin(value = "0", inclusive = false) @Digits(integer = 16, fraction = 2) BigDecimal amount,
        @DecimalMin(value = "0", inclusive = false) @Digits(integer = 16, fraction = 2) BigDecimal tenderedAmount,
        @Size(max = 100) String referenceNo,
        @Size(max = 255) String notes
) {

    public PaymentSpec toSpec() {
        return new PaymentSpec(method, Money.ofNullable(amount), Money.ofNullable(tenderedAmount), referenceNo, notes,
                clientRef);
    }
}
