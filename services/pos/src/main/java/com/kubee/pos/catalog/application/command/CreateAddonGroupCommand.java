package com.kubee.pos.catalog.application.command;

import com.kubee.pos.catalog.domain.AddonSpec;
import com.kubee.pos.common.cqrs.Command;

import java.util.List;

public record CreateAddonGroupCommand(String name, int minSelect, Integer maxSelect, List<AddonSpec> addons) implements Command<String> {
}
