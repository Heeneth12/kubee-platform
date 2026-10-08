package com.kubee.security;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** The signed-in user, built from the auth service's access token. */
@Getter
public class JwtAuthentication extends AbstractAuthenticationToken {

    private final Long userId;
    private final String userUuid;
    private final String email;
    private final Long tenantId;
    private final String tenantUuid;
    private final String userType;
    private final String roles;
    private final String accountScope;

    public JwtAuthentication(Long userId, String userUuid, String email, Long tenantId, String tenantUuid,
                             String userType, String roles, String accountScope) {
        this(userId, userUuid, email, tenantId, tenantUuid, userType, roles, accountScope, List.of());
    }

    /**
     * @param extraAuthorities service-specific authorities (see {@link JwtAuthorityContributor}),
     *                         added on top of ROLE_&lt;role key&gt; for every role in the token
     */
    public JwtAuthentication(Long userId, String userUuid, String email, Long tenantId, String tenantUuid,
                             String userType, String roles, String accountScope, Collection<String> extraAuthorities) {
        super(buildAuthorities(roles, extraAuthorities));
        this.userId = userId;
        this.userUuid = userUuid;
        this.email = email;
        this.tenantId = tenantId;
        this.tenantUuid = tenantUuid;
        this.userType = userType;
        this.roles = roles;
        this.accountScope = accountScope;
        setAuthenticated(true);
    }

    public boolean hasAuthority(String authority) {
        return getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }

    private static Collection<? extends GrantedAuthority> buildAuthorities(String roles, Collection<String> extra) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (roles != null && !roles.isEmpty()) {
            Arrays.stream(roles.split(","))
                    .filter(role -> !role.isBlank())
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.trim()))
                    .forEach(authorities::add);
        }
        if (extra != null) {
            extra.stream().map(SimpleGrantedAuthority::new).forEach(authorities::add);
        }
        return authorities;
    }

    @Override
    public Object getCredentials() {
        return null; // JWT token already validated by the filter
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }
}
