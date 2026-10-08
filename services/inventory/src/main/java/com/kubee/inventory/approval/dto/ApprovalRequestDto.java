package com.kubee.inventory.approval.dto;


import com.kubee.inventory.approval.entity.ApprovalStatus;
import com.kubee.inventory.approval.entity.ApprovalType;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRequestDto {
    private Long id;
    private String approvalRequestNumber;
    private ApprovalType approvalType;
    private Long referenceId;
    private String referenceCode;
    private ApprovalStatus status;
    private Long requestedBy;
    private String description;
    private BigDecimal valueAmount;
    private Long actionedBy;
    private String actionRemarks;
    private Date approvedDate;
    private Date createdAt;
    private Date updatedAt;
}
