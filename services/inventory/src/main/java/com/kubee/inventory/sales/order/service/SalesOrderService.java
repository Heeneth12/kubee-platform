package com.kubee.inventory.sales.order.service;

import com.kubee.inventory.sales.order.dto.SalesOrderDto;
import com.kubee.inventory.sales.order.dto.SalesOrderFilter;
import com.kubee.inventory.sales.order.dto.SalesConversionReportDto;
import com.kubee.inventory.sales.order.dto.SalesOrderStats;
import com.kubee.inventory.sales.order.entity.SalesOrderStatus;
import com.kubee.inventory.utils.common.CommonFilter;
import com.kubee.common.CommonResponse;
import com.kubee.common.CommonException;
import org.springframework.data.domain.Page;

import java.util.List;

public interface SalesOrderService {

    CommonResponse<?> createSalesOrder(SalesOrderDto dto) throws CommonException;

    CommonResponse<?> updateSalesOrder(Long id, SalesOrderDto dto) throws CommonException;

    SalesOrderDto getSalesOrderById(Long id) throws CommonException;

    CommonResponse<?> updateStatus(Long id, SalesOrderStatus status) throws CommonException;

    Page<SalesOrderDto> getAllSalesOrders(SalesOrderFilter filter, int page, int size) throws CommonException;

    List<SalesOrderDto> getAllSalesOrders(SalesOrderFilter filter) throws CommonException;

    SalesOrderStats getStats(CommonFilter filter) throws CommonException;

    byte[] downloadSalesOrdersExcel(SalesOrderFilter filter) throws CommonException;

    List<SalesConversionReportDto> getSalesOrderConversionReport(CommonFilter filter) throws CommonException;

}
