package com.kubee.inventory.stock.dto;

import com.kubee.inventory.stock.entity.AdjustmentStatus;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockAdjustmentListDto {
    private Long id;
    private String adjustmentNumber;
    private Date adjustmentDate;
    private AdjustmentStatus status;
    private Long warehouseId;
    private String reference;
    private int totalItems;
}
