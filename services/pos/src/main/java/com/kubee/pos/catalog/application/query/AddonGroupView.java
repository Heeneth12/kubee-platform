package com.kubee.pos.catalog.application.query;

import java.util.List;

public record AddonGroupView(
        String uuid,
        String name,
        int minSelect,
        Integer maxSelect,
        boolean active,
        List<AddonView> addons
) {
}
