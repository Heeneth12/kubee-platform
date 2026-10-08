package com.kubee.pos.billing.domain;

import com.kubee.pos.common.domain.DomainException;
import com.kubee.pos.common.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BillTest {

    private static final BillSeller SHOP = new BillSeller("kubee pos", "Hyderabad", "36ABCDE1234F1Z5", "36");
    private static final List<TaxComponentSpec> GST_5 = List.of(
            new TaxComponentSpec(11L, TaxType.CGST, "CGST", new BigDecimal("2.5")),
            new TaxComponentSpec(12L, TaxType.SGST, "SGST", new BigDecimal("2.5")));

    /** 105 inclusive of 5% GST = 100 taxable + 5 tax. */
    private static BillLineSpec line(List<TaxComponentSpec> components) {
        return line("105", "100", "5", "5", components);
    }

    private static BillLineSpec line(String total, String taxable, String tax, String rate,
                                     List<TaxComponentSpec> components) {
        return new BillLineSpec(1L, 1L, "Veg Burger", "+ Extra Cheese", "996331", "PCS", BigDecimal.ONE,
                Money.of(total), Money.of(total), Money.ZERO, new BigDecimal(rate), Money.of(taxable), Money.of(tax),
                Money.of(total), components);
    }

    private static BillTotals totals(String taxable, String tax, String roundOff, String grand) {
        Money total = Money.of(taxable).plus(Money.of(tax));
        return new BillTotals(total, Money.ZERO, Money.of(taxable), Money.of(tax), Money.of(roundOff), Money.of(grand));
    }

    private static Bill issue(BillBuyer buyer, List<BillLineSpec> lines, BillTotals totals) {
        return Bill.issue(7L, "KPA/26-27/000001", SHOP, buyer, totals, lines, "user-1");
    }

    // ---------------------------------------------------------------- tax split

    @Test
    void sameStateSplitsIntoCgstAndSgst() {
        Bill bill = issue(BillBuyer.NONE, List.of(line(GST_5)), totals("100", "5", "0", "105"));

        assertThat(bill.isInterState()).isFalse();
        assertThat(bill.getPlaceOfSupply()).isEqualTo("36");
        List<BillLineTax> taxes = bill.getLines().getFirst().getTaxes();
        assertThat(taxes).extracting(BillLineTax::getTaxType).containsExactly(TaxType.CGST, TaxType.SGST);
        assertThat(taxes).extracting(BillLineTax::getTaxAmount).containsExactly(Money.of("2.5"), Money.of("2.5"));
        assertThat(taxes.getFirst().getTaxComponentId()).isEqualTo(11L);
    }

    @Test
    void otherStateGstinBecomesIgst() {
        var buyer = new BillBuyer("Acme", null, "29abcde1234f1z5", null);
        Bill bill = issue(buyer, List.of(line(GST_5)), totals("100", "5", "0", "105"));

        assertThat(bill.getCustomerGstin()).isEqualTo("29ABCDE1234F1Z5");
        assertThat(bill.getPlaceOfSupply()).isEqualTo("29");
        assertThat(bill.isInterState()).isTrue();
        List<BillLineTax> taxes = bill.getLines().getFirst().getTaxes();
        assertThat(taxes).singleElement().satisfies(t -> {
            assertThat(t.getTaxType()).isEqualTo(TaxType.IGST);
            assertThat(t.getRate()).isEqualByComparingTo("5");
            assertThat(t.getTaxAmount()).isEqualTo(Money.of("5"));
        });
    }

    @Test
    void oddPaiseGoToTheLastPartSoTheSplitAddsUp() {
        // Tax 0.51 cannot be halved: CGST takes the rounded 2.5% of 10.00 (0.25), SGST the rest (0.26).
        var odd = line("10.51", "10.00", "0.51", "5", GST_5);
        Bill bill = issue(BillBuyer.NONE, List.of(odd), totals("10.00", "0.51", "0.49", "11"));

        List<BillLineTax> taxes = bill.getLines().getFirst().getTaxes();
        assertThat(taxes.get(0).getTaxAmount()).isEqualTo(Money.of("0.25"));
        assertThat(taxes.get(1).getTaxAmount()).isEqualTo(Money.of("0.26"));
    }

    @Test
    void fallsBackToEvenSplitWhenGroupChangedSinceTheSale() {
        // Sold at 5%, but the group is 12% today: keep 5%, split evenly.
        var changed = List.of(new TaxComponentSpec(1L, TaxType.CGST, "CGST", new BigDecimal("6")),
                new TaxComponentSpec(2L, TaxType.SGST, "SGST", new BigDecimal("6")));
        Bill bill = issue(BillBuyer.NONE, List.of(line(changed)), totals("100", "5", "0", "105"));

        List<BillLineTax> taxes = bill.getLines().getFirst().getTaxes();
        assertThat(taxes).extracting(BillLineTax::getRate).usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("2.5"), new BigDecimal("2.5"));
        assertThat(taxes).extracting(BillLineTax::getTaxComponentId).containsOnlyNulls();
    }

    @Test
    void cessIsKeptOnTopOfGst() {
        var withCess = List.of(new TaxComponentSpec(1L, TaxType.CGST, "CGST", new BigDecimal("14")),
                new TaxComponentSpec(2L, TaxType.SGST, "SGST", new BigDecimal("14")),
                new TaxComponentSpec(3L, TaxType.CESS, "Cess", new BigDecimal("12")));

        assertThat(BillLine.components(withCess, new BigDecimal("40"), true))
                .extracting(TaxComponentSpec::type).containsExactly(TaxType.IGST, TaxType.CESS);
    }

    @Test
    void untaxedLineHasNoTaxRows() {
        Bill bill = issue(BillBuyer.NONE, List.of(line("50", "50", "0", "0", List.of())), totals("50", "0", "0", "50"));
        assertThat(bill.getLines().getFirst().getTaxes()).isEmpty();
    }

    // ---------------------------------------------------------------- issue rules

    @Test
    void rejectsTotalsThatDoNotMatchTheLines() {
        assertThatThrownBy(() -> issue(BillBuyer.NONE, List.of(line(GST_5)), totals("100", "5", "0", "110")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("do not add up");
    }

    @Test
    void validatesGstinAndNeedsShopName() {
        var badGstin = new BillBuyer(null, null, "12345", null);
        assertThatThrownBy(() -> issue(badGstin, List.of(line(GST_5)), totals("100", "5", "0", "105")))
                .hasMessageContaining("GSTIN");

        var noName = new BillSeller(" ", null, null, null);
        assertThatThrownBy(() -> Bill.issue(7L, "A/26-27/000001", noName, BillBuyer.NONE,
                totals("100", "5", "0", "105"), List.of(line(GST_5)), null))
                .hasMessageContaining("Shop name");
    }

    @Test
    void cancelOnceAndCountPrints() {
        Bill bill = issue(BillBuyer.NONE, List.of(line(GST_5)), totals("100", "5", "0", "105"));
        bill.recordPrint();
        bill.recordPrint();
        assertThat(bill.getPrintCount()).isEqualTo(2);

        bill.cancel("wrong customer GSTIN", "user-2");
        assertThat(bill.getStatus()).isEqualTo(BillStatus.CANCELLED);
        assertThatThrownBy(() -> bill.cancel("again", "u")).hasMessageContaining("already cancelled");
        assertThatThrownBy(() -> bill.recordShare(ShareChannel.WHATSAPP)).hasMessageContaining("cancelled");
        bill.recordPrint(); // reprinting a cancelled bill is allowed (it prints as CANCELLED)
    }

    // ---------------------------------------------------------------- numbering and words

    @Test
    void financialYearStartsInApril() {
        assertThat(FinancialYear.of(LocalDate.of(2026, 10, 5), 4).key()).isEqualTo("2026-27");
        assertThat(FinancialYear.of(LocalDate.of(2027, 3, 31), 4).key()).isEqualTo("2026-27");
        assertThat(FinancialYear.of(LocalDate.of(2027, 4, 1), 4).shortLabel()).isEqualTo("27-28");
        assertThat(FinancialYear.of(LocalDate.of(2099, 12, 1), 4).shortLabel()).isEqualTo("99-00");
    }

    @Test
    void billNumberFitsGstsSixteenCharacters() {
        var year = new FinancialYear(2026);
        assertThat(BillNumber.format("KP", "A", year, 123)).isEqualTo("KPA/26-27/000123");
        assertThat(BillNumber.format(null, "A1", year, 1)).isEqualTo("A1/26-27/000001");
        assertThatThrownBy(() -> BillNumber.format("KUBEE", "A", year, 1)).hasMessageContaining("16");
        assertThat(BillNumber.normaliseSeries(" b2 ")).isEqualTo("B2");
        assertThatThrownBy(() -> BillNumber.normaliseSeries("A/1")).isInstanceOf(DomainException.class);
    }

    @Test
    void amountInIndianWords() {
        assertThat(AmountInWords.of(Money.of("437"))).isEqualTo("Rupees Four Hundred Thirty Seven Only");
        assertThat(AmountInWords.of(Money.of("123405.50")))
                .isEqualTo("Rupees One Lakh Twenty Three Thousand Four Hundred Five and Fifty Paise Only");
        assertThat(AmountInWords.of(Money.of("25000000"))).isEqualTo("Rupees Two Crore Fifty Lakh Only");
        assertThat(AmountInWords.of(Money.ZERO)).isEqualTo("Rupees Zero Only");
    }
}
