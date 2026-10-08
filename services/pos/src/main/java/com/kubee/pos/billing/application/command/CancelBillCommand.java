package com.kubee.pos.billing.application.command;

import com.kubee.pos.common.cqrs.Command;

public record CancelBillCommand(String billUuid, String reason) implements Command<Void> {
}
