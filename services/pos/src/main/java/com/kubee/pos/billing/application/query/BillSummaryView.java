package com.kubee.pos.billing.application.query;

import com.kubee.pos.billing.domain.BillStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BillSummaryView(
        String uuid,
        String billNumber,
        LocalDateTime billDate,
        BillStatus status,
        String orderUuid,
        String orderNumber,
        String customerName,
        String customerPhone,
        String customerGstin,
        BigDecimal taxableAmount,
        BigDecimal taxAmount,
        BigDecimal grandTotal,
        int printCount
) {
}
