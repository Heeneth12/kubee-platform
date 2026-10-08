package com.kubee.pos.ordering.application.command;

import com.kubee.pos.common.cqrs.Command;

public record CancelOrderCommand(String orderUuid, String reason) implements Command<Void> {
}
