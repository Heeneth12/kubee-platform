package com.kubee.pos.ordering.domain;

/** State of one payment row. PENDING is for gateway UPI waiting for confirmation (not used yet). */
public enum TransactionStatus {
    PENDING, SUCCESS, FAILED, CANCELLED
}
