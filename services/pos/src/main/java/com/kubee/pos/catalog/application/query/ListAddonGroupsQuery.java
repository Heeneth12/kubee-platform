package com.kubee.pos.catalog.application.query;

import com.kubee.pos.common.cqrs.Query;

import java.util.List;

public record ListAddonGroupsQuery(Boolean active) implements Query<List<AddonGroupView>> {
}
