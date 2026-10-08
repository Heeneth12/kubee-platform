package com.kubee.pos.billing.domain;

/** CGST + SGST inside the shop's state, IGST across states, CESS on top for some goods. */
public enum TaxType {
    CGST, SGST, IGST, CESS, OTHER
}
