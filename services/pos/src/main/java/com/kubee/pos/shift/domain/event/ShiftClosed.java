package com.kubee.pos.shift.domain.event;

import com.kubee.pos.common.domain.DomainEvent;
import com.kubee.pos.common.domain.Money;

public record ShiftClosed(String shiftUuid, Money expectedCash, Money countedCash, Money difference)
        implements DomainEvent {
}
