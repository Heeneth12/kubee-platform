package com.kubee.pos.catalog.application.command;

import com.kubee.pos.catalog.domain.AddonGroup;
import com.kubee.pos.catalog.domain.AddonGroupRepository;
import com.kubee.pos.catalog.domain.CatalogConstraints;
import com.kubee.pos.common.application.ConflictException;
import com.kubee.pos.common.cqrs.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class CreateAddonGroupHandler implements CommandHandler<CreateAddonGroupCommand, String> {

    private final AddonGroupRepository addonGroupRepository;
    private final CatalogConstraints constraints;

    @Override
    @Transactional
    public String handle(CreateAddonGroupCommand command) {
        AddonGroup group = AddonGroup.create(command.name(), command.minSelect(), command.maxSelect(), command.addons());
        if (constraints.isAddonGroupNameTaken(group.getName(), null)) {
            throw new ConflictException("Add-on group " + group.getName() + " already exists");
        }
        return addonGroupRepository.save(group).getUuid();
    }
}
