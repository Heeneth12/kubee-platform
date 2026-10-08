package com.kubee.inventory.purchase.returns.service;

import com.kubee.inventory.purchase.returns.dto.PurchaseReturnDto;
import com.kubee.inventory.purchase.returns.dto.PurchaseReturnFilter;
import com.kubee.inventory.purchase.returns.entity.ReturnStatus;
import com.kubee.common.CommonResponse;
import com.kubee.common.CommonException;
import org.springframework.data.domain.Page;

public interface PurchaseReturnService {

    CommonResponse<?> createPurchaseReturn(PurchaseReturnDto dto) throws CommonException;

    CommonResponse<?> updateStatus(Long returnId, ReturnStatus newStatus) throws CommonException;

    CommonResponse<?> updatePurchaseReturn(Long returnId, PurchaseReturnDto dto) throws CommonException;

    PurchaseReturnDto getReturnDetails(Long returnId) throws CommonException;

    Page<PurchaseReturnDto> getAllReturns(Integer page, Integer size, PurchaseReturnFilter filter) throws CommonException;
}
