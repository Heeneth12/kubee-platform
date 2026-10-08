package com.kubee.inventory.purchase.grn.dto;

import com.kubee.inventory.purchase.grn.entity.GrnStatus;
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
public class GrnFilter extends CommonFilter {
    private Long vendorId;
    private List<GrnStatus> grnStatuses;
}
