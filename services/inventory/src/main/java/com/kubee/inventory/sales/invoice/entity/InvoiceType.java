package com.kubee.inventory.sales.invoice.entity;

public enum InvoiceType {
    GST_INVOICE,          // Normal GST invoice
    BILL_OF_SUPPLY,       // Composition dealer / exempt supply
    RETAIL,
    WHOLESALE,
    CASH,
    CREDIT
}