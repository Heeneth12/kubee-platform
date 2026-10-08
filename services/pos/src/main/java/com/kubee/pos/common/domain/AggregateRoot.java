package com.kubee.pos.common.domain;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Root of an aggregate: the only entity repositories load and save.
 * Events registered here are published by Spring Data when the repository's save() is called.
 */
@MappedSuperclass
public abstract class AggregateRoot extends BaseEntity {

    @Transient
    private final transient List<DomainEvent> domainEvents = new ArrayList<>();

    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    /** Events raised since the last save (read-only copy). */
    @DomainEvents
    public Collection<DomainEvent> domainEvents() {
        return List.copyOf(domainEvents);
    }

    @AfterDomainEventPublication
    protected void clearDomainEvents() {
        domainEvents.clear();
    }
}
