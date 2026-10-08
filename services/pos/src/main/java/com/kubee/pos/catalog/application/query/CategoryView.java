package com.kubee.pos.catalog.application.query;

public record CategoryView(
        String uuid,
        String name,
        String parentUuid,
        String imageUrl,
        int sortOrder,
        boolean active,
        long itemCount
) {
}
