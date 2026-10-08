package com.kubee.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Turns a valid access token from the auth service into a {@link JwtAuthentication} and fills the request {@link UserContext}. */
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserContext userContext;
    private final List<JwtAuthorityContributor> authorityContributors;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);

            if (jwtTokenProvider.validateToken(token) && jwtTokenProvider.isAccessToken(token)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                Long tenantId = jwtTokenProvider.getTenantIdFromToken(token);
                String userUuid = jwtTokenProvider.getUserUuidFromToken(token);
                String tenantUuid = jwtTokenProvider.getTenantUuidFromToken(token);
                String email = jwtTokenProvider.getEmailFromToken(token);
                String userType = jwtTokenProvider.getUserTypeFromToken(token);
                String roles = jwtTokenProvider.getRolesFromToken(token);
                String accountScope = jwtTokenProvider.getAccountScopeFromToken(token);

                userContext.setUserId(userId);
                userContext.setUserUuid(userUuid);
                userContext.setEmail(email);
                userContext.setTenantId(tenantId);
                userContext.setTenantUuid(tenantUuid);
                userContext.setUserType(userType);
                userContext.setRoles(roles);
                userContext.setAccountScope(accountScope);

                List<String> extraAuthorities = authorityContributors.stream()
                        .flatMap(contributor -> contributor.authorities(userType, roles).stream())
                        .toList();

                JwtAuthentication authentication = new JwtAuthentication(
                        userId, userUuid, email, tenantId, tenantUuid, userType, roles, accountScope, extraAuthorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
