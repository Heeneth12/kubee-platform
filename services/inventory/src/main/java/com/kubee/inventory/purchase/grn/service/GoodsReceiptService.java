package com.kubee.inventory.purchase.grn.service;

import com.kubee.inventory.purchase.grn.dto.GrnDto;
import com.kubee.inventory.purchase.grn.dto.GrnFilter;
import com.kubee.common.CommonResponse;
import com.kubee.common.CommonException;
import org.springframework.data.domain.Page;

import java.util.List;

public interface GoodsReceiptService {

    CommonResponse createAndApproveGrn(GrnDto dto) throws CommonException;

    GrnDto getGrnDetails(Long grnId) throws CommonException;

    List<GrnDto> getGrnHistoryForPo(Long purchaseOrderId);

    Page<GrnDto> getAllGrns(Integer page, Integer size, GrnFilter filter);
}
