package com.kubee.auth.common.entity;

public enum AddressType {
    BILLING,
    SHIPPING,
    REGISTERED,     // registered / legal address (used in auth.addresses V8 schema)
    OPERATIONAL,    // operational branch address (used in auth.addresses V8 schema)
    OFFICE,
    HOME,
    OTHER
}