package com.kubee.pos.catalog.application.command;

import com.kubee.pos.catalog.domain.AddonGroup;
import com.kubee.pos.catalog.domain.AddonGroupRepository;
import com.kubee.pos.catalog.domain.CatalogConstraints;
import com.kubee.pos.common.application.ConflictException;
import com.kubee.pos.common.application.NotFoundException;
import com.kubee.pos.common.cqrs.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class UpdateAddonGroupHandler implements CommandHandler<UpdateAddonGroupCommand, String> {

    private final AddonGroupRepository addonGroupRepository;
    private final CatalogConstraints constraints;

    @Override
    @Transactional
    public String handle(UpdateAddonGroupCommand command) {
        AddonGroup group = addonGroupRepository.findByUuid(command.addonGroupUuid())
                .orElseThrow(() -> NotFoundException.of("Add-on group", command.addonGroupUuid()));
        group.update(command.name(), command.minSelect(), command.maxSelect(), command.active(), command.addons());
        if (constraints.isAddonGroupNameTaken(group.getName(), group.getId())) {
            throw new ConflictException("Add-on group " + group.getName() + " already exists");
        }
        return addonGroupRepository.save(group).getUuid();
    }
}
