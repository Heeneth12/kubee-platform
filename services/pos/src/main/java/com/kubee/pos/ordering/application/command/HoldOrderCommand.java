package com.kubee.pos.ordering.application.command;

import com.kubee.pos.common.cqrs.Command;

public record HoldOrderCommand(String orderUuid) implements Command<Void> {
}
