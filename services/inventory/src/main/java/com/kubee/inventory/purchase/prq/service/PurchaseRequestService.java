package com.kubee.inventory.purchase.prq.service;

import com.kubee.inventory.purchase.prq.dto.PurchaseRequestDto;
import com.kubee.inventory.purchase.prq.dto.PurchaseRequestFilter;
import com.kubee.inventory.purchase.prq.entity.PrqStatus;
import com.kubee.common.CommonResponse;
import com.kubee.common.CommonException;
import org.springframework.data.domain.Page;

public interface PurchaseRequestService {

    CommonResponse<?> createPrq(PurchaseRequestDto dto) throws CommonException;
    CommonResponse<?> updateStatus(Long prqId, PrqStatus status) throws CommonException;
    CommonResponse<?> updatePrq(Long prqId, PurchaseRequestDto dto) throws CommonException;
    Page<PurchaseRequestDto> getAllPrqs(Integer page, Integer size, PurchaseRequestFilter filter) throws CommonException;
    PurchaseRequestDto getPrqById(Long prqId) throws CommonException;
}
