package com.kubee.auth.subscription.dto;

import com.kubee.auth.subscription.entity.SubscriptionStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MiniSubscriptionDto {
    private Long id;
    private Long applicationId;
    private SubscriptionStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean isValid;
    private long daysRemaining;
}
