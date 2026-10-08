package com.kubee.pos.reporting.application.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Report 4: GST for the shop's CA, from ISSUED bills dated in the period (cancelled bills excluded).
 * Laid out like GSTR-1: totals, by rate, B2B invoice list, B2C by state and rate, HSN summary.
 * {@code invoiceValue} on rate/HSN rows excludes round-off; on totals and B2B rows it is the bill total.
 *
 * @param ordersWithoutBill completed orders in the period that have no issued bill (their tax is not in here)
 */
public record GstReport(
        LocalDate from,
        LocalDate to,
        Totals totals,
        List<RateRow> byRate,
        List<B2bInvoice> b2bInvoices,
        List<B2cRow> b2c,
        List<HsnRow> hsn,
        long ordersWithoutBill,
        long cancelledBills
) {

    public record Totals(long invoiceCount, BigDecimal taxableAmount, BigDecimal cgst, BigDecimal sgst,
                         BigDecimal igst, BigDecimal cess, BigDecimal totalTax, BigDecimal roundOff,
                         BigDecimal invoiceValue) {
    }

    public record RateRow(BigDecimal rate, BigDecimal taxableAmount, BigDecimal cgst, BigDecimal sgst,
                          BigDecimal igst, BigDecimal cess, BigDecimal totalTax, BigDecimal invoiceValue) {
    }

    /** One invoice to a GST-registered buyer. */
    public record B2bInvoice(String billNumber, LocalDateTime billDate, String customerGstin, String customerName,
                             String placeOfSupply, BigDecimal taxableAmount, BigDecimal cgst, BigDecimal sgst,
                             BigDecimal igst, BigDecimal cess, BigDecimal invoiceValue) {
    }

    /** Sales to unregistered buyers, grouped by place of supply (state code) and rate. */
    public record B2cRow(String placeOfSupply, BigDecimal rate, BigDecimal taxableAmount, BigDecimal cgst,
                         BigDecimal sgst, BigDecimal igst, BigDecimal cess) {
    }

    public record HsnRow(String hsnSacCode, String unitOfMeasure, BigDecimal rate, BigDecimal quantity,
                         BigDecimal taxableAmount, BigDecimal cgst, BigDecimal sgst, BigDecimal igst,
                         BigDecimal cess, BigDecimal totalValue) {
    }
}
