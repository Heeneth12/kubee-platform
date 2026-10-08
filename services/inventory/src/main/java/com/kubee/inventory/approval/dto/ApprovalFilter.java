package com.kubee.inventory.approval.dto;

import com.kubee.inventory.approval.entity.ApprovalStatus;
import com.kubee.inventory.approval.entity.ApprovalType;
import com.kubee.inventory.utils.common.CommonFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalFilter extends CommonFilter {
    private List<ApprovalStatus> approvalStatuses;
    private List<ApprovalType> approvalTypes;
    private String referenceCode;
}
