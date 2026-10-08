package com.kubee.security;

import com.kubee.common.CommonException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

/** Static access to the signed-in user of the current request. */
public class UserContextUtil {

    private UserContextUtil() {
        // private constructor to prevent object creation
    }

    private static JwtAuthentication getAuth() {
        Object authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthentication auth) {
            return auth;
        }
        return null;
    }

    public static Long getUserId() {
        JwtAuthentication auth = getAuth();
        return auth != null ? auth.getUserId() : null;
    }

    public static String getUserUuid() {
        JwtAuthentication auth = getAuth();
        return auth != null ? auth.getUserUuid() : null;
    }

    public static Long getTenantId() {
        JwtAuthentication auth = getAuth();
        return auth != null ? auth.getTenantId() : null;
    }

    public static String getTenantUuid() {
        JwtAuthentication auth = getAuth();
        return auth != null ? auth.getTenantUuid() : null;
    }

    public static String getEmail() {
        JwtAuthentication auth = getAuth();
        return auth != null ? auth.getEmail() : null;
    }

    public static Long getTenantIdOrThrow() throws CommonException {
        Long tenantId = getTenantId();
        if (tenantId == null) {
            throw new CommonException("Tenant id missing in request", HttpStatus.UNAUTHORIZED);
        }
        return tenantId;
    }

    public static Long getUserIdOrThrow() throws CommonException {
        Long userId = getUserId();
        if (userId == null) {
            throw new CommonException("User id missing in request", HttpStatus.UNAUTHORIZED);
        }
        return userId;
    }

    public static String getUserUuidOrThrow() throws CommonException {
        String userUuid = getUserUuid();
        if (userUuid == null) {
            throw new CommonException("User UUID missing in request", HttpStatus.UNAUTHORIZED);
        }
        return userUuid;
    }

    public static String getTenantUuidOrThrow() throws CommonException {
        String tenantUuid = getTenantUuid();
        if (tenantUuid == null) {
            throw new CommonException("Tenant UUID missing in request", HttpStatus.UNAUTHORIZED);
        }
        return tenantUuid;
    }

    public static String getAccountScope() {
        JwtAuthentication auth = getAuth();
        return auth != null ? auth.getAccountScope() : null;
    }

    /** Kubee's own team (account scope PLATFORM), as opposed to a customer's users. */
    public static boolean isPlatformUser() {
        return "PLATFORM".equals(getAccountScope());
    }

    public static void requirePlatformAccess() throws CommonException {
        if (!isPlatformUser()) {
            throw new CommonException("Access denied", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Allows platform users, or tenant users acting on their own tenant.
     */
    public static void requireTenantAccess(Long tenantId) throws CommonException {
        if (isPlatformUser()) {
            return;
        }
        if (tenantId == null || !tenantId.equals(getTenantId())) {
            throw new CommonException("Access denied", HttpStatus.FORBIDDEN);
        }
    }
}
