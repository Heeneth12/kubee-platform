package com.kubee.pos.billing.application.query;

import com.kubee.pos.billing.domain.BillStatus;
import com.kubee.pos.billing.domain.ShareChannel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Everything printed on the GST invoice. {@code interState} = IGST bill. */
public record BillView(
        String uuid,
        String billNumber,
        LocalDateTime billDate,
        BillStatus status,
        String orderUuid,
        String orderNumber,
        Seller seller,
        Buyer buyer,
        boolean interState,
        BigDecimal subTotal,
        BigDecimal discountAmount,
        BigDecimal taxableAmount,
        BigDecimal taxAmount,
        BigDecimal roundOffAmount,
        BigDecimal grandTotal,
        String amountInWords,
        List<BillLineView> lines,
        List<TaxSummaryView> taxSummary,
        List<HsnSummaryView> hsnSummary,
        int printCount,
        ShareChannel sharedVia,
        String issuedBy,
        LocalDateTime cancelledAt,
        String cancelledBy,
        String cancelReason
) {

    public record Seller(String name, String address, String gstin, String stateCode) {
    }

    public record Buyer(String name, String phone, String gstin, String placeOfSupply) {
    }
}
