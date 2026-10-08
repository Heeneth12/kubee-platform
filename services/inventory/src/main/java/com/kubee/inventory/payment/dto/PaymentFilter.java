package com.kubee.inventory.payment.dto;

import com.kubee.inventory.payment.entity.enums.PaymentStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentFilter {
    private Long id;
    private Long customerId;
    private PaymentStatus status;
    private String paymentMethod;
    private String paymentNumber;
}
