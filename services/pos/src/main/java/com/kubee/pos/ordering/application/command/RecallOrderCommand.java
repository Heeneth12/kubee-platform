package com.kubee.pos.ordering.application.command;

import com.kubee.pos.common.cqrs.Command;

public record RecallOrderCommand(String orderUuid) implements Command<Void> {
}
