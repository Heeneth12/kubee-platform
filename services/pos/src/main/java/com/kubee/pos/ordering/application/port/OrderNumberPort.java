package com.kubee.pos.ordering.application.port;

/** Hands out order tokens ("1", "2", ...) that restart every day, per shop. */
public interface OrderNumberPort {

    /** Next token for today. Runs in the caller's transaction, so a rolled-back order does not use up a number. */
    String nextOrderNumber();
}
