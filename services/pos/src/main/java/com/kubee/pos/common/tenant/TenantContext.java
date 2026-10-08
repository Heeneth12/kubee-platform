package com.kubee.pos.common.tenant;

import java.util.Optional;

/** Who is calling: the shop (tenant) and user, set once per request by {@link TenantFilter}. */
public final class TenantContext {

    public record Caller(String tenantUuid, String userUuid) {
    }

    private static final ThreadLocal<Caller> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Caller caller) {
        CURRENT.set(caller);
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static Optional<String> currentTenantUuid() {
        return Optional.ofNullable(CURRENT.get()).map(Caller::tenantUuid);
    }

    public static Optional<String> currentUserUuid() {
        return Optional.ofNullable(CURRENT.get()).map(Caller::userUuid);
    }

    public static String requireTenantUuid() {
        return currentTenantUuid()
                .orElseThrow(() -> new IllegalStateException("No tenant in context"));
    }
}
