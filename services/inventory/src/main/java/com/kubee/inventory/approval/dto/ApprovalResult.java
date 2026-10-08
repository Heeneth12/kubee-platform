package com.kubee.inventory.approval.dto;

import com.kubee.inventory.approval.entity.ApprovalResultStatus;

public class ApprovalResult {
    private ApprovalResultStatus status;
    private Long approvalRequestId;
    private String message;
}
