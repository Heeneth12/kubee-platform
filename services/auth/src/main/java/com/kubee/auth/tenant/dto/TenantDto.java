package com.kubee.auth.tenant.dto;


import com.kubee.auth.common.dto.AddressDto;
import com.kubee.auth.common.dto.ApplicationDto;
import com.kubee.auth.subscription.dto.SubscriptionDto;
import com.kubee.auth.user.dto.UserMiniDto;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantDto {
    private Long id;
    private String tenantUuid;
    private String tenantName;
    private String tenantCode;
    private String email;
    private String phone;
    private Boolean isActive;
    private UserMiniDto tenantAdmin;
    private Set<ApplicationDto> applications;
    private Set<AddressDto> tenantAddress;
    private TenantDetailsDto tenantDetails;
    private SubscriptionDto subscription;
}
