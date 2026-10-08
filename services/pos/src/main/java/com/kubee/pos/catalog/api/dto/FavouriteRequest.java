package com.kubee.pos.catalog.api.dto;

import jakarta.validation.constraints.NotNull;

public record FavouriteRequest(@NotNull Boolean favourite) {
}
