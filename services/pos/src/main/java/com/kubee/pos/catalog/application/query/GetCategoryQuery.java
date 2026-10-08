package com.kubee.pos.catalog.application.query;

import com.kubee.pos.common.cqrs.Query;

public record GetCategoryQuery(String categoryUuid) implements Query<CategoryView> {
}
