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
class ChangeItemFavouriteHandler implements CommandHandler<ChangeItemFavouriteCommand, Void> {

    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public Void handle(ChangeItemFavouriteCommand command) {
        Item item = itemRepository.findByUuid(command.itemUuid())
                .orElseThrow(() -> NotFoundException.of("Item", command.itemUuid()));
        item.changeFavourite(command.favourite());
        itemRepository.save(item);
        return null;
    }
}
