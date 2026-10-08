package com.kubee.inventory.sales.delivery.dto;

import com.kubee.inventory.contacts.dto.AddressDto;
import com.kubee.inventory.employee.dto.EmployeeDto;
import com.kubee.inventory.sales.delivery.entity.ShipmentStatus;
import com.kubee.inventory.sales.delivery.entity.ShipmentType;
import com.kubee.inventory.sales.invoice.dto.InvoiceDto;
import com.kubee.inventory.utils.common.dto.UserAddressDto;
import com.kubee.inventory.utils.common.dto.UserMiniDto;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryDto {
    private Long id;
    private Long tenantId;
    private String deliveryNumber;  // DEV-2025-001
    private InvoiceDto invoice;
    private UserMiniDto contactMini;
    private Long customerId;
    private String customerName;
    private ShipmentType type;   // PICKUP / COURIER / OWN_FLEET
    private ShipmentStatus status; // PENDING, SCHEDULED, SHIPPED, DELIVERED
    private EmployeeDto deliveryPerson;
    private String remarks;
    private Date scheduledDate;
    private Date shippedDate;
    private Date deliveredDate;
    private String deliveryAddress;
    private String contactPerson;
    private String contactPhone;
    private String attachmentUuid;
}
