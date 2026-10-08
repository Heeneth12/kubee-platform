package com.kubee.inventory.sales.returns.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnItemRequest {
    private Long itemId;
    private String batchNumber;
    private Integer quantity;
}
