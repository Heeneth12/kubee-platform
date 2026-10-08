package com.kubee.pos.billing.domain.event;

import com.kubee.pos.common.domain.DomainEvent;

public record BillIssued(String billUuid, String billNumber) implements DomainEvent {
}
