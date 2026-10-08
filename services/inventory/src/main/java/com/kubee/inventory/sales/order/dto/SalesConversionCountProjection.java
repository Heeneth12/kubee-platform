package com.kubee.inventory.sales.order.dto;

public interface SalesConversionCountProjection {
    Long getTotalSalesOrders();
    Long getConvertedToInvoice();
}
