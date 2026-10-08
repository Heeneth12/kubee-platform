package com.kubee.pos.ordering.domain;

/**
 * How the money moved. CARD is only recorded; the card machine is separate.
 * AGGREGATOR = the customer paid Zomato / Swiggy online and the platform settles with the shop later;
 * only allowed on online orders.
 */
public enum PaymentMethod {
    CASH, UPI, CARD, WALLET, BANK_TRANSFER, OTHER, AGGREGATOR
}
