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
class DeleteItemHandler implements CommandHandler<DeleteItemCommand, Void> {

    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public Void handle(DeleteItemCommand command) {
        Item item = itemRepository.findByUuid(command.itemUuid())
                .orElseThrow(() -> NotFoundException.of("Item", command.itemUuid()));
        item.delete();
        itemRepository.save(item);
        return null;
    }
}
