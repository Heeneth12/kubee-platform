package com.kubee.pos.ordering.domain.event;

import com.kubee.pos.common.domain.DomainEvent;
import com.kubee.pos.common.domain.Money;
import com.kubee.pos.ordering.domain.PaymentMethod;

public record PaymentRefunded(String orderUuid, String refundUuid, String originalPaymentUuid,
                              PaymentMethod method, Money amount) implements DomainEvent {
}
