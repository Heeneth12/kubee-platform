package com.kubee.pos.ordering.domain;

import com.kubee.pos.common.domain.Money;

/**
 * Money received at the counter.
 *
 * @param amount         amount to put against the order; for CASH it may be null when
 *                       {@code tenderedAmount} is given (then amount = min(tendered, due))
 * @param tenderedAmount CASH only: what the customer handed over; the change is worked out
 * @param clientRef      idempotency key from the device (offline sync / double tap)
 */
public record PaymentSpec(
        PaymentMethod method,
        Money amount,
        Money tenderedAmount,
        String referenceNo,
        String notes,
        String clientRef
) {
}
