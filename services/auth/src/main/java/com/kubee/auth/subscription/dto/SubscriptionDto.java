package com.kubee.auth.subscription.dto;


import com.kubee.auth.subscription.entity.Subscription;
import com.kubee.auth.subscription.entity.SubscriptionStatus;

import lombok.*;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionDto {
    private Long id;
    private SubscriptionPlanDto plan;
    private SubscriptionStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean autoRenew = false;
    private LocalDateTime createdAt;
    private boolean isValid;
    private long daysRemaining;
}
