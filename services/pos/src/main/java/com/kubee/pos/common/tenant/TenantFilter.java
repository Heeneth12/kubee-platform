package com.kubee.pos.common.tenant;

import com.kubee.security.JwtAuthentication;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Puts the caller's tenant/user into {@link TenantContext} for every /api/** request,
 * taken from the auth service JWT (tenantUuid / userUuid claims) that {@code JwtAuthFilter} has already validated.
 * <p>
 * Runs as a plain servlet filter, i.e. after the Spring Security chain.
 */
@Component
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthentication auth)
                || auth.getTenantUuid() == null || auth.getTenantUuid().isBlank()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":401,\"message\":\"Token has no tenant\"}");
            return;
        }
        try {
            TenantContext.set(new TenantContext.Caller(auth.getTenantUuid(), auth.getUserUuid()));
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
