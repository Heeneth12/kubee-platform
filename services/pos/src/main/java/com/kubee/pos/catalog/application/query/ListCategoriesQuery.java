package com.kubee.pos.catalog.application.query;

import com.kubee.pos.common.cqrs.Query;

import java.util.List;

public record ListCategoriesQuery(Boolean active) implements Query<List<CategoryView>> {
}
