package com.kubee.auth.subscription.dto;

import lombok.*;

import java.time.LocalDateTime;

/** A tenant's pending plan change, as the platform sees it in the activation queue. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanRequestDto {
    private Long subscriptionId;
    private Long tenantId;
    private String tenantName;
    private String tenantCode;
    private SubscriptionPlanDto requestedPlan;
    private SubscriptionPlanDto currentPlan;
    private LocalDateTime currentPlanEndsAt;
    private LocalDateTime requestedAt;
}
