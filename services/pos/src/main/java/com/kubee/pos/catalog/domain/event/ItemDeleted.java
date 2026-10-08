package com.kubee.pos.catalog.domain.event;

import com.kubee.pos.common.domain.DomainEvent;

public record ItemDeleted(String itemUuid) implements DomainEvent {
}
