package com.kubee.inventory.approval.dto;

import com.kubee.inventory.approval.entity.ApprovalStatus;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor 
public class ApprovalActionDto {
    private Long requestId;
    private ApprovalStatus status; // APPROVED or REJECTED
    private String remarks;
}
