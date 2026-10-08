package com.kubee.pos.catalog.application.command;

import com.kubee.pos.common.cqrs.Command;

public record DeleteAddonGroupCommand(String addonGroupUuid) implements Command<Void> {
}
