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
class DeleteAddonGroupHandler implements CommandHandler<DeleteAddonGroupCommand, Void> {

    private final AddonGroupRepository addonGroupRepository;
    private final CatalogConstraints constraints;

    @Override
    @Transactional
    public Void handle(DeleteAddonGroupCommand command) {
        AddonGroup group = addonGroupRepository.findByUuid(command.addonGroupUuid())
                .orElseThrow(() -> NotFoundException.of("Add-on group", command.addonGroupUuid()));
        if (constraints.isAddonGroupInUse(group.getId())) {
            throw new ConflictException("Add-on group is attached to items; remove it from those items first");
        }
        group.delete();
        addonGroupRepository.save(group);
        return null;
    }
}
