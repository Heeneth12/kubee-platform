package com.kubee.inventory.purchase.grn.entity;

public enum GrnStatus {
    DRAFT,
    PENDING_QA, // Truck unloaded, checking quality
    RECEIVED,   // Stock Updated (IN)
    CANCELLED
}