package com.kubee.pos.ordering.domain.event;

import com.kubee.pos.common.domain.DomainEvent;
import com.kubee.pos.common.domain.Money;
import com.kubee.pos.ordering.domain.PaymentMethod;

public record PaymentReceived(String orderUuid, String paymentUuid, PaymentMethod method, Money amount)
        implements DomainEvent {
}
