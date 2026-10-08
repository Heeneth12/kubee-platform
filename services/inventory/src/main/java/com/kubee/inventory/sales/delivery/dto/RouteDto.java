package com.kubee.inventory.sales.delivery.dto;

import com.kubee.inventory.sales.delivery.entity.RouteStatus;
import lombok.*;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteDto {
    private Long id;
    private String routeNumber;
    private String areaName; // Generic label for the trip
    private Long employeeId;
    private String employeeName;
    private String vehicleNumber;
    private RouteStatus status;
    private Date startDate;
    private List<DeliveryDto> deliveries;
}
