package com.kubee.pos.common.security;

import com.kubee.security.JwtAuthorityContributor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * POS has two levels: staff (cashier) and manager (owner / admin / manager). Managers additionally get the
 * {@link #MANAGE} authority, which guards reports, catalog changes, refunds and bill cancellation
 * (see {@link SecurityConfig}). Who counts as a manager comes from the auth service token: its role keys, or a
 * SUPER_ADMIN / ADMIN user type.
 */
@Component
public class PosPermissions implements JwtAuthorityContributor {

    public static final String MANAGE = "POS_MANAGE";

    private static final Set<String> MANAGER_USER_TYPES = Set.of("SUPER_ADMIN", "ADMIN");

    private final Set<String> managerRoles;

    public PosPermissions(@Value("${pos.security.manager-roles:SUPER_ADMIN,ADMIN,OWNER,MANAGER}") String managerRoles) {
        this.managerRoles = split(managerRoles);
    }

    /** Adds {@link #MANAGE} to the signed-in user when they are a manager (called by the shared JwtAuthFilter). */
    @Override
    public Collection<String> authorities(String userType, String roles) {
        return isManager(userType, roles) ? List.of(MANAGE) : List.of();
    }

    public boolean isManager(String userType, String roles) {
        if (userType != null && MANAGER_USER_TYPES.contains(userType.trim().toUpperCase(Locale.ROOT))) {
            return true;
        }
        return split(roles).stream().anyMatch(managerRoles::contains);
    }

    private static Set<String> split(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
