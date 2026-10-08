package com.kubee.inventory.payment.dto;

import com.kubee.inventory.payment.entity.enums.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreditNoteRefundRequestDto {
    private Long creditNoteId;
    private BigDecimal refundAmount;
    private PaymentMethod refundMethod;    // how the cash is returned
    private String refundReferenceNumber;
    private String remarks;
}
