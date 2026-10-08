package com.kubee.inventory.sales.delivery.dto;

import com.kubee.inventory.sales.delivery.entity.ShipmentStatus;
import com.kubee.inventory.sales.delivery.entity.ShipmentType;
import com.kubee.inventory.utils.common.CommonFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;


@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryFilterDto extends CommonFilter {
    private Long deliveryId;
    private Long customerId;
    private Long invoiceId;
    private String deliveryNumber;
    private List<ShipmentType> shipmentTypes;
    private List<ShipmentStatus> shipmentStatuses;
    private List<Long> deliveryIds;
    private List<Long> invoiceIds;
}
