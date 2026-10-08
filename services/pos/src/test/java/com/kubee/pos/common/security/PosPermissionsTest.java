package com.kubee.pos.common.security;

import com.kubee.security.JwtAuthentication;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class PosPermissionsTest {

    private final PosPermissions permissions = new PosPermissions("SUPER_ADMIN, admin,OWNER,MANAGER");

    @Test
    void ownersAdminsAndManagersManage() {
        assertThat(permissions.isManager("EMPLOYEE", "MANAGER")).isTrue();
        assertThat(permissions.isManager("EMPLOYEE", "CASHIER,Admin")).isTrue();
        assertThat(permissions.isManager("SUPER_ADMIN", "")).isTrue();
        assertThat(permissions.isManager("admin", null)).isTrue();
    }

    @Test
    void cashiersDoNot() {
        assertThat(permissions.isManager("EMPLOYEE", "CASHIER")).isFalse();
        assertThat(permissions.isManager(null, null)).isFalse();
        assertThat(permissions.isManager("EMPLOYEE", "MANAGERS")).isFalse();
    }

    @Test
    void managersGetThePosManageAuthority() {
        // The shared JwtAuthFilter builds the authentication with PosPermissions' extra authorities
        var manager = new JwtAuthentication(1L, "u", "e", 1L, "t", "EMPLOYEE", "MANAGER", null,
                permissions.authorities("EMPLOYEE", "MANAGER"));
        var cashier = new JwtAuthentication(2L, "u", "e", 1L, "t", "EMPLOYEE", "CASHIER", null,
                permissions.authorities("EMPLOYEE", "CASHIER"));

        assertThat(manager.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_MANAGER", PosPermissions.MANAGE);
        assertThat(cashier.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CASHIER");
    }
}
