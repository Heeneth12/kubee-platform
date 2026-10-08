package com.kubee.inventory.approval.dto;

import com.kubee.inventory.approval.entity.ApprovalType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalCheckContext {
    private ApprovalType type;      // What are we checking?
    // The data to check against rules
    private BigDecimal amount;      // e.g. Invoice Total
    private Double percentage;      // e.g. Discount % or Tax Variance %

    // Reference data for the log
    private Long referenceId;       // Invoice ID
    private String referenceCode;   // Invoice Number
}
