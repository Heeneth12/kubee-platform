package com.kubee.pos.common.tenant;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Feeds the current tenant to Hibernate so every entity with {@code @TenantId} is
 * stamped on insert and filtered on every query automatically.
 */
@Component
class HibernateTenantResolver implements CurrentTenantIdentifierResolver<String>, HibernatePropertiesCustomizer {

    /** Used outside a request (startup, jobs): matches no rows, so nothing can leak. */
    static final String NO_TENANT = "__no_tenant__";

    @Override
    public String resolveCurrentTenantIdentifier() {
        return TenantContext.currentTenantUuid().orElse(NO_TENANT);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
