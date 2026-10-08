package com.kubee.inventory.purchase.returns.dto;

import com.kubee.inventory.purchase.returns.entity.ReturnStatus;
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
public class PurchaseReturnFilter extends CommonFilter {
    private List<ReturnStatus> prStatuses;
    private Long vendorId;
}
