package com.kubee.pos.billing.domain.event;

import com.kubee.pos.common.domain.DomainEvent;

public record BillCancelled(String billUuid, String billNumber, String reason) implements DomainEvent {
}
