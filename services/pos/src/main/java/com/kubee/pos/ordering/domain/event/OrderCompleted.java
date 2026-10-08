package com.kubee.pos.ordering.domain.event;

import com.kubee.pos.common.domain.DomainEvent;

/** Fully paid and closed. The billing module will issue the GST invoice from this. */
public record OrderCompleted(String orderUuid) implements DomainEvent {
}
