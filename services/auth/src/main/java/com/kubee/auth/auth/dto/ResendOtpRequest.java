package com.kubee.auth.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResendOtpRequest {

    @NotNull(message = "Tenant ID is required")
    private Long tenantId;
}
