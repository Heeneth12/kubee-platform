package com.kubee.pos.shift.application.port;

import com.kubee.pos.common.domain.Money;

import java.time.LocalDateTime;

/** What the shift needs from payments: net cash taken in a time window. */
public interface CashSalesPort {

    /** Successful CASH payments minus CASH refunds with payment time in [from, to). */
    Money netCash(LocalDateTime from, LocalDateTime to);
}
