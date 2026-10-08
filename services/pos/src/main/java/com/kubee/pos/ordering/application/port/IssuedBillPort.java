package com.kubee.pos.ordering.application.port;

import java.util.Optional;

/** What ordering needs from billing: is there a live GST invoice for this order? */
public interface IssuedBillPort {

    /** Bill number of the order's ISSUED bill, if any. */
    Optional<String> issuedBillNumber(Long orderId);
}
