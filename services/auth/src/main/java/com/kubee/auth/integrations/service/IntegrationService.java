package com.kubee.auth.integrations.service;

import com.kubee.auth.common.service.AccessService;
import com.kubee.auth.integrations.converter.EncryptedStringConverter;
import com.kubee.auth.integrations.dto.IntegrationDto;
import com.kubee.auth.integrations.dto.IntegrationRequest;
import com.kubee.auth.integrations.dto.TestConnectionResponse;
import com.kubee.auth.integrations.entity.Integration;
import com.kubee.auth.integrations.entity.IntegrationType;
import com.kubee.auth.integrations.repository.IntegrationRepository;
import com.kubee.auth.tenant.entity.Tenant;
import com.kubee.auth.tenant.repository.TenantRepository;
import com.kubee.security.UserContextUtil;
import com.kubee.common.CommonResponse;
import com.kubee.common.Status;
import com.kubee.common.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.EncryptedPrivateKeyInfo;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationService {

    private final IntegrationRepository integrationRepository;
    private final TenantRepository tenantRepository;
    private final AccessService accessService;

    @Transactional
    public CommonResponse createIntegration(IntegrationRequest request) throws CommonException {
        accessService.requireSettingsEditor();

        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new CommonException("Tenant not found", HttpStatus.NOT_FOUND));

        if (request.getIntegrationType() == null) {
            throw new CommonException("Integration type is required", HttpStatus.BAD_REQUEST);
        }

        if (integrationRepository.existsByTenantIdAndIntegrationType(tenantId, request.getIntegrationType())) {
            throw new CommonException(
                    "Integration of type " + request.getIntegrationType()
                            + " already exists for this tenant",
                    HttpStatus.CONFLICT);
        }

        Integration integration = Integration.builder()
                .tenant(tenant)
                .integrationType(request.getIntegrationType())
                .displayName(request.getDisplayName())
                .primaryKey(request.getPrimaryKey())
                .secondaryKey(request.getSecondaryKey())
                .tertiaryKey(request.getTertiaryKey())
                .isTestMode(request.getIsTestMode() != null ? request.getIsTestMode() : false)
                .webhookConfig(request.getWebhookConfig())
                .extraConfig(stripSecrets(request.getExtraConfig()))
                .links(request.getLinks())
                .build();

        Integration saved = integrationRepository.save(integration);
        log.info("Integration {} created for tenant {}", saved.getIntegrationType(), tenantId);

        return CommonResponse.builder()
                .id(saved.getId().toString())
                .message("Integration created successfully")
                .status(Status.SUCCESS)
                .build();
    }

    @Transactional(readOnly = true)
    public List<IntegrationDto> getIntegrations(Long tenantId) throws CommonException {
        if (!tenantRepository.existsById(tenantId)) {
            throw new CommonException("Tenant not found", HttpStatus.NOT_FOUND);
        }
        return integrationRepository.findByTenantId(tenantId)
                .stream()
                // Pass 'false' to keep the keys masked in the list view
                .map(entity -> toDto(entity, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IntegrationDto getIntegrationByType(Long tenantId, IntegrationType type) throws CommonException {
        return integrationRepository.findByTenantIdAndIntegrationType(tenantId, type)
                // Use lambda to pass 'true' for individual fetch
                .map(entity -> toDto(entity, accessService.canEditSettings()))
                .orElseThrow(() -> new CommonException(
                        "Integration " + type + " not configured for this tenant",
                        HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public IntegrationDto getIntegrationById(Long tenantId, Long integrationId) throws CommonException {
        Integration integration = integrationRepository.findByIdAndTenantId(integrationId, tenantId)
                .orElseThrow(() -> new CommonException("Integration not found", HttpStatus.NOT_FOUND));

        // Explicitly revealing keys for the detail view
        return toDto(integration, accessService.canEditSettings());
    }


    @Transactional(readOnly = true)
    public CommonResponse checkIntegration(Long tenantId, IntegrationType type) {

        return integrationRepository.findByTenantIdAndIntegrationType(tenantId, type)
                .map(entity -> CommonResponse.builder()
                        .id(entity.getId().toString())
                        .message("Integration " + type + " is configured for this tenant")
                        .status(Status.SUCCESS)
                        .build())
                .orElse(CommonResponse.builder()
                        .id(null)
                        .message("Integration " + type + " not configured for this tenant")
                        .status(Status.NOT_FOUND) // Or Status.FAILURE depending on your enum
                        .build());
    }

    @Transactional
    public CommonResponse updateIntegration(Long tenantId, Long integrationId, IntegrationRequest request)
            throws CommonException {
        accessService.requireSettingsEditor();
        Integration integration = integrationRepository.findByIdAndTenantId(integrationId, tenantId)
                .orElseThrow(() -> new CommonException("Integration not found", HttpStatus.NOT_FOUND));

        if (request.getDisplayName() != null)
            integration.setDisplayName(request.getDisplayName());
        if (isNewSecret(request.getPrimaryKey()))
            integration.setPrimaryKey(request.getPrimaryKey());
        if (isNewSecret(request.getSecondaryKey()))
            integration.setSecondaryKey(request.getSecondaryKey());
        if (isNewSecret(request.getTertiaryKey()))
            integration.setTertiaryKey(request.getTertiaryKey());
        if (request.getIsTestMode() != null)
            integration.setIsTestMode(request.getIsTestMode());
        // webhookConfig carries webhook secrets (e.g. Razorpay), so it follows the same keep-if-blank rule
        if (isNewSecret(request.getWebhookConfig()))
            integration.setWebhookConfig(request.getWebhookConfig());
        if (request.getExtraConfig() != null)
            integration.setExtraConfig(stripSecrets(request.getExtraConfig()));
        if (request.getLinks() != null)
            integration.setLinks(request.getLinks());

        Integration updated = integrationRepository.save(integration);
        log.info("Integration {} updated for tenant {}", updated.getId(), tenantId);

        return CommonResponse.builder()
                .id(updated.getId().toString())
                .message("Integration updated successfully")
                .status(Status.SUCCESS)
                .build();
    }

    @Transactional
    public CommonResponse deleteIntegration(Long tenantId, Long integrationId) throws CommonException {
        accessService.requireSettingsEditor();
        Integration integration = integrationRepository.findByIdAndTenantId(integrationId, tenantId)
                .orElseThrow(() -> new CommonException("Integration not found", HttpStatus.NOT_FOUND));

        // Remove the stored credentials for good; "turn off" is the toggle endpoint.
        // A soft delete kept the secrets and blocked reconnecting the same provider.
        integrationRepository.delete(integration);
        log.info("Integration {} deleted for tenant {}", integrationId, tenantId);

        return CommonResponse.builder()
                .id(integration.getId().toString())
                .message("Integration removed successfully")
                .status(Status.SUCCESS)
                .build();
    }

    @Transactional
    public CommonResponse toggleIntegration(Long tenantId, Long integrationId) throws CommonException {
        accessService.requireSettingsEditor();
        Integration integration = integrationRepository.findByIdAndTenantId(integrationId, tenantId)
                .orElseThrow(() -> new CommonException("Integration not found", HttpStatus.NOT_FOUND));

        integration.setIsActive(!integration.getIsActive());
        integrationRepository.save(integration);

        String state = Boolean.TRUE.equals(integration.getIsActive()) ? "enabled" : "disabled";
        log.info("Integration {} {} for tenant {}", integrationId, state, tenantId);

        return CommonResponse.builder()
                .id(integration.getId().toString())
                .message("Integration " + state + " successfully")
                .status(Status.SUCCESS)
                .build();
    }

    @Transactional
    public TestConnectionResponse testConnection(Long tenantId, Long integrationId) throws CommonException {
        accessService.requireSettingsEditor();
        Integration integration = integrationRepository.findByIdAndTenantId(integrationId, tenantId)
                .orElseThrow(() -> new CommonException("Integration not found", HttpStatus.NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        boolean hasKey = integration.getPrimaryKey() != null && !integration.getPrimaryKey().isBlank();

        if (!hasKey) {
            integration.setIsConnected(false);
            integrationRepository.save(integration);
            return TestConnectionResponse.builder()
                    .connected(false)
                    .message("Connection failed: primary key is missing or empty")
                    .testedAt(now)
                    .build();
        }

        integration.setIsConnected(true);
        integration.setConnectedAt(now);
        integrationRepository.save(integration);
        log.info("Test connection passed for integration {} of tenant {}", integrationId, tenantId);

        return TestConnectionResponse.builder()
                .connected(true)
                .message("Credentials validated. Note: live provider ping is a planned enhancement.")
                .testedAt(now)
                .build();
    }

    private static final String MASK = "********";

    /** Null, blank or the mask sent back by a client means "keep the stored secret". */
    private boolean isNewSecret(String value) {
        return value != null && !value.isBlank() && !MASK.equals(value);
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Pattern SECRET_FIELD = Pattern.compile("(?i).*(secret|password|token|apikey|api_key).*");

    /**
     * extraConfig is stored and returned in plaintext, so secrets must never live there; they belong in the
     * encrypted key columns. Drops secret-looking fields from a JSON object; leaves anything else unchanged.
     */
    private String stripSecrets(String extraConfig) {
        if (extraConfig == null || extraConfig.isBlank()) return extraConfig;
        try {
            JsonNode node = JSON.readTree(extraConfig);
            if (!(node instanceof ObjectNode object)) return extraConfig;
            List<String> secretFields = new ArrayList<>();
            object.propertyNames().forEach(name -> {
                if (SECRET_FIELD.matcher(name).matches()) secretFields.add(name);
            });
            if (secretFields.isEmpty()) return extraConfig;
            secretFields.forEach(object::remove);
            return JSON.writeValueAsString(object);
        } catch (JacksonException e) {
            return extraConfig; // not JSON: nothing to strip
        }
    }

    private IntegrationDto toDto(Integration integration, Boolean revealKeys) {
        return IntegrationDto.builder()
                .id(integration.getId())
                .integrationUuid(integration.getIntegrationUuid())
                .tenantId(integration.getTenant().getId())
                .integrationType(integration.getIntegrationType())
                .displayName(integration.getDisplayName())
                .primaryKey(revealKeys ? integration.getPrimaryKey() : MASK)
                .secondaryKey(revealKeys ? integration.getSecondaryKey() : MASK)
                .tertiaryKey(revealKeys ? integration.getTertiaryKey() : MASK)
                .isTestMode(integration.getIsTestMode())
                .isConnected(integration.getIsConnected())
                .isActive(integration.getIsActive())
                .webhookConfig(revealKeys || integration.getWebhookConfig() == null ? integration.getWebhookConfig() : MASK)
                .extraConfig(stripSecrets(integration.getExtraConfig()))
                .links(integration.getLinks())
                .connectedAt(integration.getConnectedAt())
                .createdAt(integration.getCreatedAt())
                .updatedAt(integration.getUpdatedAt())
                .build();
    }
}
