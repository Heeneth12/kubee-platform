package com.kubee.pos.billing.domain;

import java.util.Optional;

public interface BillRepository {

    Bill save(Bill bill);

    Optional<Bill> findByUuid(String uuid);

    /** The one ISSUED bill of an order, if any. */
    Optional<Bill> findFirstByOrderIdAndStatus(Long orderId, BillStatus status);
}
