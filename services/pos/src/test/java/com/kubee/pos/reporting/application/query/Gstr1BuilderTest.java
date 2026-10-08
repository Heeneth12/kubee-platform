package com.kubee.pos.reporting.application.query;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class Gstr1BuilderTest {

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    private static final Gstr1Data DATA = new Gstr1Data("36AAAAA0000A1Z5", "36", YearMonth.of(2026, 10),
            List.of(
                    new Gstr1Data.InvoiceRate("KPA/26-27/000002", LocalDate.of(2026, 10, 5), "29ABCDE1234F1Z5", "29",
                            d("439.00"), d("5.000"), d("400.00"), d("20.00"), d("0"), d("0"), d("0")),
                    new Gstr1Data.InvoiceRate("KPA/26-27/000002", LocalDate.of(2026, 10, 5), "29ABCDE1234F1Z5", "29",
                            d("439.00"), d("18.000"), d("16.10"), d("2.90"), d("0"), d("0"), d("0"))),
            List.of(),
            List.of(new Gstr1Data.B2cs("36", true, d("5.000"), d("161.90"), d("0"), d("4.04"), d("4.06"), d("0"))),
            List.of(new Gstr1Data.Nil(false, true, d("50.00"))),
            List.of(new Gstr1Data.Hsn(false, "996331", "PLATE", d("5.000"), d("2.000"), d("80"), d("76.19"), d("0"),
                            d("1.90"), d("1.91"), d("0")),
                    new Gstr1Data.Hsn(true, "22021010", "BTL", d("18.000"), d("1.000"), d("19"), d("16.10"), d("2.90"),
                            d("0"), d("0"), d("0"))),
            List.of(new Gstr1Data.DocSeries("KPA", "KPA/26-27/000001", "KPA/26-27/000004", 4, 1)));

    @Test
    void headerAndPeriod() {
        Map<String, Object> json = Gstr1Builder.build(DATA);
        assertThat(json).containsEntry("gstin", "36AAAAA0000A1Z5").containsEntry("fp", "102026");
        assertThat(json.keySet()).containsExactly("gstin", "fp", "b2b", "b2cl", "b2cs", "nil", "hsn", "doc_issue");
    }

    @Test
    void b2bGroupsRatesUnderOneInvoice() {
        var b2b = (List<Map<String, Object>>) Gstr1Builder.build(DATA).get("b2b");
        assertThat(b2b).singleElement().satisfies(c -> {
            assertThat(c.get("ctin")).isEqualTo("29ABCDE1234F1Z5");
            var inv = ((List<Map<String, Object>>) c.get("inv")).getFirst();
            assertThat(inv).containsEntry("inum", "KPA/26-27/000002").containsEntry("idt", "05-10-2026")
                    .containsEntry("pos", "29").containsEntry("rchrg", "N").containsEntry("inv_typ", "R");
            var items = (List<Map<String, Object>>) inv.get("itms");
            assertThat(items).hasSize(2);
            var first = (Map<String, Object>) items.getFirst().get("itm_det");
            assertThat(first.get("rt")).isEqualTo(new BigDecimal("5"));
            assertThat(first.get("iamt")).isEqualTo(new BigDecimal("20.00"));
        });
    }

    @Test
    void b2csNilHsnAndDocuments() {
        Map<String, Object> json = Gstr1Builder.build(DATA);
        var b2cs = ((List<Map<String, Object>>) json.get("b2cs")).getFirst();
        assertThat(b2cs).containsEntry("sply_ty", "INTRA").containsEntry("typ", "OE").containsEntry("pos", "36");

        var nil = ((List<Map<String, Object>>) ((Map<String, Object>) json.get("nil")).get("inv")).getFirst();
        assertThat(nil).containsEntry("sply_ty", "INTRAB2C").containsEntry("nil_amt", new BigDecimal("50.00"));

        var hsn = (Map<String, Object>) json.get("hsn");
        var b2c = ((List<Map<String, Object>>) hsn.get("hsn_b2c")).getFirst();
        assertThat(b2c).containsEntry("hsn_sc", "996331").containsEntry("uqc", "NA").containsEntry("qty", 0)
                .containsEntry("val", new BigDecimal("80.00"));
        var b2bHsn = ((List<Map<String, Object>>) hsn.get("hsn_b2b")).getFirst();
        assertThat(b2bHsn).containsEntry("uqc", "BTL");

        var docs = (List<Map<String, Object>>) ((Map<String, Object>) json.get("doc_issue")).get("doc_det");
        var series = ((List<Map<String, Object>>) docs.getFirst().get("docs")).getFirst();
        assertThat(series).containsEntry("totnum", 4L).containsEntry("cancel", 1L).containsEntry("net_issue", 3L);
    }
}
