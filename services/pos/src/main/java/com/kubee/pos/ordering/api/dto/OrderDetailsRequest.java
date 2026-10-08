package com.kubee.pos.ordering.api.dto;

import com.kubee.pos.ordering.domain.CustomerDetails;
import com.kubee.pos.ordering.domain.OrderSource;
import com.kubee.pos.ordering.domain.OrderType;
import jakarta.validation.constraints.Size;

/** Full replace: fields left out are cleared (order type and source stay as they were when left out). */
public record OrderDetailsRequest(
        OrderType orderType,
        @Size(max = 255) String customerName,
        @Size(max = 20) String customerPhone,
        @Size(max = 50) String tableLabel,
        String notes,
        OrderSource source,
        @Size(max = 64) String externalOrderId
) {

    public CustomerDetails toDetails() {
        return new CustomerDetails(orderType, customerName, customerPhone, tableLabel, notes, source, externalOrderId);
    }
}
