package com.kubee.inventory.sales.order.repository;

import java.time.LocalDate;

public interface SalesConversionDateProjection {
    LocalDate getReportDate();
    Long getTotalSalesOrders();
    Long getConvertedToInvoice();
    java.math.BigDecimal getTotalSalesValue();
    java.math.BigDecimal getConvertedSalesValue();
    Long getPendingApprovalCount();
    Long getCancelledRejectedCount();
}
