package com.kubee.inventory.sales.delivery.service;

import com.kubee.inventory.sales.delivery.dto.*;
import com.kubee.inventory.sales.invoice.dto.InvoiceDto;
import com.kubee.inventory.sales.invoice.entity.Invoice;
import com.kubee.common.CommonResponse;
import com.kubee.common.CommonException;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DeliveryService {

    Page<DeliveryDto> getAllDeliveries(int page, int size, DeliveryFilterDto filter) throws CommonException;

    DeliveryDto getDeliveryDetail(Long deliveryId) throws CommonException;

    List<DeliveryDto> searchDeliveryDetails(DeliveryFilterDto filter) throws CommonException;

    void createDeliveryForInvoice(Invoice invoice, InvoiceDto dto);

    CommonResponse<?> updateDeliveryStatus(Long id, DeliveryStatusUpdateRequest request, MultipartFile file) throws CommonException;

    CommonResponse<?> createRoute(RouteCreateDto dto) throws CommonException;

    RouteDto getRouteDetail(Long routeId) throws CommonException;

    CommonResponse<?> completeRoute(Long routeId) throws CommonException;

    CommonResponse<?> startRoute(Long routeId) throws CommonException;

    Page<RouteDto> getAllRoutes(int page, int size) throws CommonException;

    RouteSummaryDto getRouteSummary() throws CommonException;

    List<BulkDeliveryItemDto> getBulkDeliveryItems(DeliveryFilterDto filter) throws CommonException;

    byte[] downloadBulkDeliveryItemsExcel(DeliveryFilterDto filter) throws CommonException;
}
