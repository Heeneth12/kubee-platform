package com.kubee.pos.ordering.domain;

/**
 * Optional details the cashier can fill in or change at any time before the order is cancelled.
 * {@code source} and {@code orderType} keep their current value when null; {@code externalOrderId} is the
 * Zomato / Swiggy order id and is only allowed on online orders.
 */
public record CustomerDetails(
        OrderType orderType,
        String customerName,
        String customerPhone,
        String tableLabel,
        String notes,
        OrderSource source,
        String externalOrderId
) {

    public static final CustomerDetails NONE = new CustomerDetails(null, null, null, null, null, null, null);

    public CustomerDetails(OrderType orderType, String customerName, String customerPhone, String tableLabel,
                           String notes) {
        this(orderType, customerName, customerPhone, tableLabel, notes, null, null);
    }
}
