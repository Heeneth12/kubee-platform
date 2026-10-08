package com.kubee.pos.reporting.application.query;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Shapes {@link Gstr1Data} into the JSON the GST portal's offline tool imports (sections b2b, b2cl, b2cs, nil,
 * hsn, doc_issue). Field names follow the GSTN GSTR-1 JSON schema. Import the file into the offline tool and
 * check it there before filing: the schema changes from time to time.
 */
public final class Gstr1Builder {

    /** B2C invoices to another state above this value go invoice-wise into B2CL (₹1 lakh since Aug 2024). */
    public static final BigDecimal B2CL_LIMIT = new BigDecimal("100000");

    private static final DateTimeFormatter INVOICE_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter RETURN_PERIOD = DateTimeFormatter.ofPattern("MMyyyy");

    /** Our units of measure → GST Unit Quantity Codes (UQC). Anything else is OTH. */
    private static final Map<String, String> UQC = Map.ofEntries(
            Map.entry("PCS", "PCS"), Map.entry("NOS", "NOS"), Map.entry("KG", "KGS"), Map.entry("KGS", "KGS"),
            Map.entry("G", "GMS"), Map.entry("GM", "GMS"), Map.entry("GMS", "GMS"), Map.entry("LTR", "LTR"),
            Map.entry("L", "LTR"), Map.entry("ML", "MLT"), Map.entry("MLT", "MLT"), Map.entry("BTL", "BTL"),
            Map.entry("BOX", "BOX"), Map.entry("PKT", "PAC"), Map.entry("PAC", "PAC"), Map.entry("DOZ", "DOZ"),
            Map.entry("MTR", "MTR"), Map.entry("SET", "SET"), Map.entry("BAG", "BAG"), Map.entry("CAN", "CAN"),
            Map.entry("PLATE", "NOS"), Map.entry("CUP", "NOS"), Map.entry("GLASS", "NOS"));

    private Gstr1Builder() {
    }

    public static Map<String, Object> build(Gstr1Data d) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("gstin", d.sellerGstin());
        json.put("fp", d.month().format(RETURN_PERIOD));
        json.put("b2b", b2b(d.b2b()));
        json.put("b2cl", b2cl(d.b2cl()));
        json.put("b2cs", d.b2cs().stream().map(Gstr1Builder::b2cs).toList());
        json.put("nil", Map.of("inv", d.nil().stream().map(Gstr1Builder::nil).toList()));
        json.put("hsn", hsn(d.hsn()));
        json.put("doc_issue", docIssue(d.documents()));
        return json;
    }

    // ---------------------------------------------------------------- sections

    private static List<Map<String, Object>> b2b(List<Gstr1Data.InvoiceRate> lines) {
        Map<String, List<Gstr1Data.InvoiceRate>> byCustomer = lines.stream()
                .collect(Collectors.groupingBy(Gstr1Data.InvoiceRate::customerGstin, LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> result = new ArrayList<>();
        byCustomer.forEach((ctin, customerLines) -> result.add(map(
                "ctin", ctin,
                "inv", invoices(customerLines, true))));
        return result;
    }

    private static List<Map<String, Object>> b2cl(List<Gstr1Data.InvoiceRate> lines) {
        Map<String, List<Gstr1Data.InvoiceRate>> byState = lines.stream()
                .collect(Collectors.groupingBy(Gstr1Data.InvoiceRate::placeOfSupply, LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> result = new ArrayList<>();
        byState.forEach((pos, stateLines) -> result.add(map("pos", pos, "inv", invoices(stateLines, false))));
        return result;
    }

    private static List<Map<String, Object>> invoices(List<Gstr1Data.InvoiceRate> lines, boolean b2b) {
        Map<String, List<Gstr1Data.InvoiceRate>> byInvoice = lines.stream()
                .collect(Collectors.groupingBy(Gstr1Data.InvoiceRate::billNumber, LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> result = new ArrayList<>();
        byInvoice.forEach((number, rates) -> {
            Gstr1Data.InvoiceRate first = rates.getFirst();
            Map<String, Object> inv = map(
                    "inum", number,
                    "idt", first.billDate().format(INVOICE_DATE),
                    "val", amount(first.invoiceValue()));
            if (b2b) {
                inv.put("pos", first.placeOfSupply());
                inv.put("rchrg", "N");
                inv.put("inv_typ", "R");
            }
            List<Map<String, Object>> items = new ArrayList<>();
            for (int i = 0; i < rates.size(); i++) {
                Gstr1Data.InvoiceRate r = rates.get(i);
                Map<String, Object> detail = map("txval", amount(r.taxable()), "rt", rate(r.rate()),
                        "iamt", amount(r.igst()));
                if (b2b) {
                    detail.put("camt", amount(r.cgst()));
                    detail.put("samt", amount(r.sgst()));
                }
                detail.put("csamt", amount(r.cess()));
                items.add(map("num", i + 1, "itm_det", detail));
            }
            inv.put("itms", items);
            result.add(inv);
        });
        return result;
    }

    private static Map<String, Object> b2cs(Gstr1Data.B2cs r) {
        return map(
                "sply_ty", r.intraState() ? "INTRA" : "INTER",
                "pos", r.placeOfSupply(),
                "typ", "OE",
                "txval", amount(r.taxable()),
                "rt", rate(r.rate()),
                "iamt", amount(r.igst()),
                "camt", amount(r.cgst()),
                "samt", amount(r.sgst()),
                "csamt", amount(r.cess()));
    }

    private static Map<String, Object> nil(Gstr1Data.Nil r) {
        // GSTN codes: INTRA = within the state, INTR = inter-state.
        return map("sply_ty", (r.intraState() ? "INTRA" : "INTR") + (r.b2b() ? "B2B" : "B2C"),
                "nil_amt", amount(r.amount()), "expt_amt", 0, "ngsup_amt", 0);
    }

    /** HSN summary, split into B2B and B2C tables as GSTR-1 asks since 2025. */
    private static Map<String, Object> hsn(List<Gstr1Data.Hsn> rows) {
        return map(
                "hsn_b2b", hsnRows(rows.stream().filter(Gstr1Data.Hsn::b2b).toList()),
                "hsn_b2c", hsnRows(rows.stream().filter(r -> !r.b2b()).toList()));
    }

    private static List<Map<String, Object>> hsnRows(List<Gstr1Data.Hsn> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Gstr1Data.Hsn r = rows.get(i);
            boolean service = r.hsnSacCode() != null && r.hsnSacCode().startsWith("99");
            result.add(map(
                    "num", i + 1,
                    "hsn_sc", r.hsnSacCode() == null ? "" : r.hsnSacCode(),
                    "desc", "",
                    "uqc", service ? "NA" : UQC.getOrDefault(upper(r.unitOfMeasure()), "OTH"),
                    "qty", service ? 0 : r.quantity().stripTrailingZeros(),
                    "rt", rate(r.rate()),
                    "val", amount(r.value()),
                    "txval", amount(r.taxable()),
                    "iamt", amount(r.igst()),
                    "camt", amount(r.cgst()),
                    "samt", amount(r.sgst()),
                    "csamt", amount(r.cess())));
        }
        return result;
    }

    /** Table 13: invoices for outward supply (document type 1), per series. */
    private static Map<String, Object> docIssue(List<Gstr1Data.DocSeries> series) {
        List<Map<String, Object>> docs = new ArrayList<>();
        for (int i = 0; i < series.size(); i++) {
            Gstr1Data.DocSeries s = series.get(i);
            docs.add(map("num", i + 1, "from", s.firstNumber(), "to", s.lastNumber(), "totnum", s.total(),
                    "cancel", s.cancelled(), "net_issue", s.total() - s.cancelled()));
        }
        return map("doc_det", docs.isEmpty() ? List.of() : List.of(map("doc_num", 1, "docs", docs)));
    }

    // ---------------------------------------------------------------- helpers

    private static BigDecimal amount(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    /** 5.000 → 5, 0.250 → 0.25 */
    private static BigDecimal rate(BigDecimal rate) {
        BigDecimal stripped = rate.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }

    private static String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    /** Ordered, mutable map from alternating key / value arguments. */
    private static Map<String, Object> map(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}
