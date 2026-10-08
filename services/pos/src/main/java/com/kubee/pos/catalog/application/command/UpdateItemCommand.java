package com.kubee.pos.catalog.application.command;

import com.kubee.pos.common.cqrs.Command;

public record UpdateItemCommand(String itemUuid, ItemInput input) implements Command<String> {
}
