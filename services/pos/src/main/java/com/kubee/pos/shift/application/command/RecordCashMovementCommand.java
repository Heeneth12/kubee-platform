package com.kubee.pos.shift.application.command;

import com.kubee.pos.common.cqrs.Command;
import com.kubee.pos.common.domain.Money;
import com.kubee.pos.shift.domain.CashMovementType;

public record RecordCashMovementCommand(String shiftUuid, CashMovementType type, Money amount, String reason)
        implements Command<Void> {
}
