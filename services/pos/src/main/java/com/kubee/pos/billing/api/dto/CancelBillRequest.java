package com.kubee.pos.billing.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelBillRequest(@NotBlank @Size(max = 255) String reason) {
}
