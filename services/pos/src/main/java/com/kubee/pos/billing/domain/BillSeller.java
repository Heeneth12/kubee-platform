package com.kubee.pos.billing.domain;

/** The shop as printed on the invoice. Copied onto the bill so later settings changes never alter it. */
public record BillSeller(String name, String address, String gstin, String stateCode) {
}
