package com.kubee.pos.catalog.application.query;

import com.kubee.pos.common.cqrs.Query;

public record LookupItemQuery(String code) implements Query<ItemLookupView> {
}
