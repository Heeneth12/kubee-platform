package com.kubee.inventory.sales.returns.dto;

import com.kubee.inventory.utils.common.CommonFilter;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SalesReturnFilter extends CommonFilter {
    private Long customerId;
    private Long invoiceId;
    private String returnNumber;
}
