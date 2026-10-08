package com.kubee.security;

import lombok.Data;

@Data
public class UserContext {
    private Long userId;
    private String userUuid;
    private String email;
    private String tenantUuid;
    private Long tenantId;
    private String userType;
    private String roles;
    private String accountScope;
}
