package com.kubee.inventory.stock.service;

import com.kubee.inventory.stock.dto.StockAdjustmentCreateDto;
import com.kubee.inventory.stock.dto.StockAdjustmentDetailDto;
import com.kubee.inventory.stock.dto.StockAdjustmentListDto;
import com.kubee.inventory.stock.dto.StockFilterDto;
import com.kubee.inventory.stock.entity.AdjustmentStatus;
import com.kubee.common.CommonResponse;
import com.kubee.common.CommonException;
import org.springframework.data.domain.Page;

public interface StockAdjustmentService {

    CommonResponse<?> createStockAdjustment(StockAdjustmentCreateDto dto) throws CommonException;

    Page<StockAdjustmentListDto> getAllStockAdjustments(StockFilterDto filter, Integer page, Integer size);

    StockAdjustmentDetailDto getStockAdjustmentById(Long id);

    void approveStockAdjustment(Long adjustmentId) throws CommonException;

    void rejectStockAdjustment(Long adjustmentId) throws CommonException;

    CommonResponse<?> updateStockAdjustmentStatus(Long adjustmentId, AdjustmentStatus status) throws CommonException;

}

