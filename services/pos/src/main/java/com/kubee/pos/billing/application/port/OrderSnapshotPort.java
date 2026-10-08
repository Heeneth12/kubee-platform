package com.kubee.pos.billing.application.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** What billing needs from ordering: a priced order with its lines, as stored. */
public interface OrderSnapshotPort {

    Optional<OrderSnapshot> findOrder(String orderUuid);

    /**
     * @param status        ordering's order status (OPEN, HELD, COMPLETED, CANCELLED)
     * @param paymentStatus ordering's payment status (UNPAID ... REFUNDED)
     */
    record OrderSnapshot(
            long id,
            String uuid,
            String orderNumber,
            String status,
            String paymentStatus,
            String customerName,
            String customerPhone,
            BigDecimal subTotal,
            BigDecimal discountAmount,
            BigDecimal taxableAmount,
            BigDecimal taxAmount,
            BigDecimal roundOffAmount,
            BigDecimal grandTotal,
            List<LineSnapshot> lines
    ) {
    }

    /**
     * @param unitPrice  item/variant price plus add-ons per unit
     * @param addonsText "+ Extra Cheese, 2 x Jalapenos" or null
     */
    record LineSnapshot(
            long orderItemId,
            Long itemId,
            String itemName,
            String variantName,
            String addonsText,
            String hsnSacCode,
            String unitOfMeasure,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal lineAmount,
            BigDecimal discountAmount,
            Long taxGroupId,
            BigDecimal taxRate,
            BigDecimal taxableAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount
    ) {
    }
}
