package com.kubee.pos.ordering.domain.event;

import com.kubee.pos.common.domain.DomainEvent;

public record OrderCancelled(String orderUuid, String reason) implements DomainEvent {
}
