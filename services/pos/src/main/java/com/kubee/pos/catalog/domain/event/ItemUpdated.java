package com.kubee.pos.catalog.domain.event;

import com.kubee.pos.common.domain.DomainEvent;

/** Any change to an item, its variants or add-on links. Billing devices use this to refresh their cache. */
public record ItemUpdated(String itemUuid) implements DomainEvent {
}
