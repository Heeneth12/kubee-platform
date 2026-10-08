package com.kubee.inventory.purchase.prq.dto;

import com.kubee.inventory.purchase.prq.entity.PrqStatus;
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
public class PurchaseRequestFilter extends CommonFilter {
    private Long vendorId;
    private List<PrqStatus> prqStatuses;
}
