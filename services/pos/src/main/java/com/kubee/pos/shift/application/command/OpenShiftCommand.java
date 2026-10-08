package com.kubee.pos.shift.application.command;

import com.kubee.pos.common.cqrs.Command;
import com.kubee.pos.common.domain.Money;

/** Start the day / shift with the cash counted in the drawer. Only one shift can be open at a time. */
public record OpenShiftCommand(Money openingCash, String notes) implements Command<String> {
}
