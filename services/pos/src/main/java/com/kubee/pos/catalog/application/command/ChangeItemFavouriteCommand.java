package com.kubee.pos.catalog.application.command;

import com.kubee.pos.common.cqrs.Command;

public record ChangeItemFavouriteCommand(String itemUuid, boolean favourite) implements Command<Void> {
}
