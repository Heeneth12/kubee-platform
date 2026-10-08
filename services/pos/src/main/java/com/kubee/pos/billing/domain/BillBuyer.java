package com.kubee.pos.billing.domain;

/**
 * Optional customer details. {@code gstin} only for B2B bills. {@code placeOfSupply} is a 2-digit GST
 * state code; when left out it is taken from the GSTIN, else the sale is treated as within the shop's state.
 */
public record BillBuyer(String name, String phone, String gstin, String placeOfSupply) {

    public static final BillBuyer NONE = new BillBuyer(null, null, null, null);
}
