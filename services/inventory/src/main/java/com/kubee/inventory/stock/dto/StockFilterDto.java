package com.kubee.inventory.stock.dto;

import com.kubee.inventory.stock.entity.AdjustmentStatus;
import com.kubee.inventory.utils.common.CommonFilter;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class StockFilterDto extends CommonFilter {
    private Long itemId;
    private List<AdjustmentStatus> stockAdjustmentStatuses;
    private String stockAdjustmentNumber;
}