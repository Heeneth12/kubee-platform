package com.kubee.pos.billing.api.dto;

import com.kubee.pos.billing.domain.ShareChannel;
import jakarta.validation.constraints.NotNull;

public record ShareBillRequest(@NotNull ShareChannel channel) {
}
