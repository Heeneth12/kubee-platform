package com.kubee.pos.catalog.application.command;

import com.kubee.pos.catalog.domain.Item;
import com.kubee.pos.catalog.domain.ItemRepository;
import com.kubee.pos.common.cqrs.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class CreateItemHandler implements CommandHandler<CreateItemCommand, String> {

    private final ItemRepository itemRepository;
    private final ItemCommandSupport support;

    @Override
    @Transactional
    public String handle(CreateItemCommand command) {
        ItemInput input = command.input();
        var resolved = support.resolve(input);
        Item item = Item.create(resolved.details(), resolved.pricing(), input.variants(), resolved.addonGroupIds());
        item.changeFavourite(input.favourite());
        item.changeActive(input.active());
        support.checkUnique(item);
        return itemRepository.save(item).getUuid();
    }
}
