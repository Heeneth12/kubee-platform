package com.kubee.pos.catalog.application.query;

import com.kubee.pos.common.cqrs.Query;

public record GetAddonGroupQuery(String addonGroupUuid) implements Query<AddonGroupView> {
}
