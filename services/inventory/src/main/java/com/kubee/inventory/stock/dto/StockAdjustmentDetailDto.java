package com.kubee.inventory.stock.dto;

import com.kubee.inventory.stock.entity.AdjustmentStatus;
import lombok.*;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockAdjustmentDetailDto {
    private Long id;
    private String adjustmentNumber;
    private Date adjustmentDate;
    private AdjustmentStatus status;
    private Long warehouseId;
    private String remarks;
    private String reference;
    private List<ItemDetail> items;
}
