package com.kubee.auth.subscription.dto;

import com.kubee.auth.subscription.entity.PlanType;
import com.kubee.auth.subscription.entity.SubscriptionPlan;
import lombok.*;


import java.math.BigDecimal;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlanDto {
    private Long id;
    private Long applicationId;
    private String name;
    private String description;
    private PlanType type;
    private BigDecimal price;
    private Integer durationDays;
    private Integer maxUsers;
    private Boolean isActive;
}
