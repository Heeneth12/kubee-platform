package com.kubee.inventory.purchase.po.service;

import com.kubee.inventory.purchase.po.dto.PurchaseOrderDto;
import com.kubee.inventory.purchase.po.dto.PurchaseOrderFilter;
import com.kubee.inventory.purchase.po.entity.PoStatus;
import com.kubee.common.CommonResponse;
import com.kubee.common.CommonException;
import org.springframework.data.domain.Page;

public interface PurchaseOrderService {

    CommonResponse createPurchaseOrder(PurchaseOrderDto dto);
    PurchaseOrderDto getPurchaseOrderById(Long id) throws CommonException;
    Page<PurchaseOrderDto> getAllPurchaseOrders(Integer page, Integer size, PurchaseOrderFilter filter);
    CommonResponse updatePurchaseOrder(Long poId, PurchaseOrderDto dto) throws CommonException;
    CommonResponse updatePurchaseOrderStatus(Long poId, PoStatus status) throws CommonException;


}
