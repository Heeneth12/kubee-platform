package com.kubee.pos.common.security;

import com.kubee.security.JwtAuthentication;

import com.kubee.pos.common.web.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lets the app know who is logged in and whether to show manager-only screens. */
@RestController
public class SessionController {

    public record SessionView(String userUuid, String email, String tenantUuid, String userType, String roles,
                              boolean manager) {
    }

    @GetMapping("/api/v1/session")
    public ResponseEntity<ApiResponse<SessionView>> session() {
        var auth = (JwtAuthentication) SecurityContextHolder.getContext().getAuthentication();
        return ApiResponse.ok(new SessionView(auth.getUserUuid(), auth.getEmail(), auth.getTenantUuid(),
                auth.getUserType(), auth.getRoles(), auth.hasAuthority(PosPermissions.MANAGE)));
    }
}
