package com.kubee.pos.ordering.domain;

/** OPEN (being billed) -> HELD (parked) -> OPEN ... -> COMPLETED (fully paid) or CANCELLED. */
public enum OrderStatus {
    OPEN, HELD, COMPLETED, CANCELLED
}
