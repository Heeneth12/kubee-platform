package com.kubee.pos.shift.application.command;

import com.kubee.pos.common.cqrs.Command;
import com.kubee.pos.common.domain.Money;

/** End the shift with the cash counted in the drawer; the server works out expected cash and the difference. */
public record CloseShiftCommand(String shiftUuid, Money countedCash, String notes) implements Command<Void> {
}
