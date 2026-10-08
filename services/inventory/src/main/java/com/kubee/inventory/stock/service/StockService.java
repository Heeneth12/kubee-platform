package com.kubee.inventory.stock.service;


import com.kubee.inventory.stock.dto.*;
import com.kubee.common.CommonResponse;
import com.kubee.common.CommonException;
import org.springframework.data.domain.Page;

import java.io.ByteArrayInputStream;
import java.util.List;

public interface StockService {

    CommonResponse<?> updateStock(StockUpdateDto stockUpdateDto);
    Page<StockDto> getCurrentStock(StockFilterDto filterDto, Integer page, Integer size);
    Page<StockLedgerDto> getStockTransactions(StockLedgerFilter filterDto, Integer page, Integer size);
    ByteArrayInputStream downloadStockLedger(StockLedgerFilter filterDto, String format);
    List<ItemStockSearchDto> searchItemsWithBatches(StockFilterDto filterDto);
    StockDashboardDto getStockDashboard(Long warehouseId) throws CommonException;

}
