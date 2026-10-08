package com.kubee.pos.ordering.domain;

/** Money position of an order, derived from its payments and refunds. */
public enum PaymentStatus {
    UNPAID, PARTIALLY_PAID, PAID, PARTIALLY_REFUNDED, REFUNDED
}
