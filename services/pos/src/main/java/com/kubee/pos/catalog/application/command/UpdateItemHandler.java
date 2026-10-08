package com.kubee.pos.catalog.application.command;

import com.kubee.pos.catalog.domain.Item;
import com.kubee.pos.catalog.domain.ItemRepository;
import com.kubee.pos.common.application.NotFoundException;
import com.kubee.pos.common.cqrs.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class UpdateItemHandler implements CommandHandler<UpdateItemCommand, String> {

    private final ItemRepository itemRepository;
    private final ItemCommandSupport support;

    @Override
    @Transactional
    public String handle(UpdateItemCommand command) {
        Item item = itemRepository.findByUuid(command.itemUuid())
                .orElseThrow(() -> NotFoundException.of("Item", command.itemUuid()));
        ItemInput input = command.input();
        var resolved = support.resolve(input);
        item.update(resolved.details(), resolved.pricing(), input.variants(), resolved.addonGroupIds());
        item.changeFavourite(input.favourite());
        item.changeActive(input.active());
        support.checkUnique(item);
        return itemRepository.save(item).getUuid();
    }
}
