package com.kubee.pos.catalog.application.query;

import com.kubee.pos.common.cqrs.Query;

public record GetItemQuery(String itemUuid) implements Query<ItemView> {
}
