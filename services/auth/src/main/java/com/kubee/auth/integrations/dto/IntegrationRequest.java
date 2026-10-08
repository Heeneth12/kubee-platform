package com.kubee.auth.integrations.dto;

import com.kubee.auth.integrations.entity.IntegrationType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IntegrationRequest {

    private IntegrationType integrationType;
    private String displayName;
    private String primaryKey;
    private String secondaryKey;
    private String tertiaryKey;
    private Boolean isTestMode;
    private String webhookConfig;
    private String extraConfig;
    private String links;
}
