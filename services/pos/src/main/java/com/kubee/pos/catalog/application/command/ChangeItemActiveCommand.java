package com.kubee.pos.catalog.application.command;

import com.kubee.pos.common.cqrs.Command;

public record ChangeItemActiveCommand(String itemUuid, boolean active) implements Command<Void> {
}
