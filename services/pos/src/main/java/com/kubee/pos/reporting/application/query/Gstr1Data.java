package com.kubee.pos.reporting.application.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Raw figures for one month's GSTR-1, from ISSUED bills. Shaped into the GSTN JSON by {@link Gstr1Builder}. */
public record Gstr1Data(
        String sellerGstin,
        String sellerStateCode,
        YearMonth month,
        List<InvoiceRate> b2b,
        List<InvoiceRate> b2cl,
        List<B2cs> b2cs,
        List<Nil> nil,
        List<Hsn> hsn,
        List<DocSeries> documents
) {

    /** One rate line of one invoice. {@code customerGstin} is null for B2CL. */
    public record InvoiceRate(String billNumber, LocalDate billDate, String customerGstin, String placeOfSupply,
                              BigDecimal invoiceValue, BigDecimal rate, BigDecimal taxable, BigDecimal igst,
                              BigDecimal cgst, BigDecimal sgst, BigDecimal cess) {
    }

    public record B2cs(String placeOfSupply, boolean intraState, BigDecimal rate, BigDecimal taxable,
                       BigDecimal igst, BigDecimal cgst, BigDecimal sgst, BigDecimal cess) {
    }

    /** 0% lines (nil-rated / exempt / non-GST: reported as nil-rated; the CA should confirm). */
    public record Nil(boolean b2b, boolean intraState, BigDecimal amount) {
    }

    public record Hsn(boolean b2b, String hsnSacCode, String unitOfMeasure, BigDecimal rate, BigDecimal quantity,
                      BigDecimal value, BigDecimal taxable, BigDecimal igst, BigDecimal cgst, BigDecimal sgst,
                      BigDecimal cess) {
    }

    /** Bill numbers used in the month for one series, e.g. KPA: KPA/26-27/000001 to 000120, 3 cancelled. */
    public record DocSeries(String series, String firstNumber, String lastNumber, long total, long cancelled) {
    }
}
