package com.kubee.pos.billing.domain;

/** An issued bill never changes; to correct it, CANCEL it and issue a new one. */
public enum BillStatus {
    ISSUED, CANCELLED
}
