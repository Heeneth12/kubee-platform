package com.kubee.inventory.purchase.po.dto;

import com.kubee.inventory.purchase.po.entity.PoStatus;
import com.kubee.inventory.utils.common.CommonFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseOrderFilter extends CommonFilter {
    private Long vendorId;
    private List<PoStatus> poStatuses;
}