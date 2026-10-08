package com.kubee.pos.ordering.application.command;

import com.kubee.pos.common.cqrs.Command;

public record RemoveOrderLineCommand(String orderUuid, String lineUuid) implements Command<Void> {
}
