package com.kubee.pos.catalog.domain;

import java.util.Optional;

public interface ItemRepository {

    Item save(Item item);

    Optional<Item> findByUuid(String uuid);
}
