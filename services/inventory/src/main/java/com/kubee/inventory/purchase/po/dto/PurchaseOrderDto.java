package com.kubee.inventory.purchase.po.dto;

import com.kubee.inventory.purchase.po.entity.PoStatus;
import com.kubee.inventory.utils.common.dto.UserMiniDto;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseOrderDto {
    private Long id;
    private Long prqId;
    private Long vendorId;
    private Long warehouseId;
    private String orderNumber;
    private Long orderDate;
    private Long expectedDeliveryDate;
    private PoStatus status;
    private String notes;
    private BigDecimal flatDiscount;
    private BigDecimal flatTax;
    private BigDecimal totalAmount;
    private BigDecimal totalDiscount;
    private BigDecimal totalTax;
    private BigDecimal grandTotal;
    private Date createdAt;
    private UserMiniDto vendorDetails;
    private List<PurchaseOrderItemDto> items;
}