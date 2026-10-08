package com.kubee.pos.billing.domain;

import com.kubee.pos.billing.domain.event.BillCancelled;
import com.kubee.pos.billing.domain.event.BillIssued;
import com.kubee.pos.common.domain.AggregateRoot;
import com.kubee.pos.common.domain.Guard;
import com.kubee.pos.common.domain.Money;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The GST invoice for a completed order. Seller, buyer, lines, tax split and totals are all snapshots:
 * an issued bill never changes. To correct it, cancel it and issue a new one (new number).
 * Only printing / sharing is recorded afterwards.
 */
@Getter
@Entity
@Table(name = "bills")
@SQLRestriction("is_deleted = false")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bill extends AggregateRoot {

    private static final Pattern GSTIN = Pattern.compile("\\d{2}[A-Z]{5}\\d{4}[A-Z][1-9A-Z]Z[0-9A-Z]");
    private static final Pattern STATE_CODE = Pattern.compile("\\d{2}");
    private static final Pattern PHONE = Pattern.compile("\\+?\\d{10,15}");

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "bill_number", nullable = false, length = 50)
    private String billNumber;

    @Column(name = "bill_date", nullable = false)
    private LocalDateTime billDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BillStatus status;

    @Column(name = "seller_name", nullable = false)
    private String sellerName;

    @Column(name = "seller_address", columnDefinition = "text")
    private String sellerAddress;

    @Column(name = "seller_gstin", length = 15)
    private String sellerGstin;

    @Column(name = "seller_state_code", length = 2)
    private String sellerStateCode;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "customer_gstin", length = 15)
    private String customerGstin;

    /** GST state code of the buyer; differs from the seller's => IGST. */
    @Column(name = "place_of_supply", length = 2)
    private String placeOfSupply;

    @Column(name = "sub_total", nullable = false)
    private Money subTotal;

    @Column(name = "discount_amount", nullable = false)
    private Money discountAmount;

    @Column(name = "taxable_amount", nullable = false)
    private Money taxableAmount;

    @Column(name = "tax_amount", nullable = false)
    private Money taxAmount;

    @Column(name = "round_off_amount", nullable = false)
    private Money roundOffAmount;

    @Column(name = "grand_total", nullable = false)
    private Money grandTotal;

    @Column(name = "print_count", nullable = false)
    private int printCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "shared_via", length = 20)
    private ShareChannel sharedVia;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    @Column(name = "issued_by", length = 36)
    private String issuedBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by", length = 36)
    private String cancelledBy;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL)
    @OrderBy("sortOrder ASC, id ASC")
    private List<BillLine> lines = new ArrayList<>();

    public static Bill issue(Long orderId, String billNumber, BillSeller seller, BillBuyer buyer, BillTotals totals,
                             List<BillLineSpec> lineSpecs, String issuedBy) {
        Guard.isTrue(orderId != null, "Order is required");
        Guard.isTrue(lineSpecs != null && !lineSpecs.isEmpty(), "A bill needs at least one line");
        Bill bill = new Bill();
        bill.orderId = orderId;
        bill.billNumber = Guard.requireText(billNumber, "Bill number", BillNumber.MAX_LENGTH);
        bill.billDate = LocalDateTime.now();
        bill.status = BillStatus.ISSUED;
        bill.issuedBy = issuedBy;
        bill.applySeller(seller);
        bill.applyBuyer(buyer == null ? BillBuyer.NONE : buyer);
        bill.applyTotals(totals);

        boolean interState = bill.isInterState();
        for (int i = 0; i < lineSpecs.size(); i++) {
            bill.lines.add(new BillLine(bill, lineSpecs.get(i), interState, i));
        }
        bill.checkLinesMatchTotals();
        bill.registerEvent(new BillIssued(bill.getUuid(), bill.billNumber));
        return bill;
    }

    public void cancel(String reason, String userUuid) {
        Guard.isTrue(status == BillStatus.ISSUED, "This bill is already cancelled");
        this.cancelReason = Guard.requireText(reason, "Cancel reason", 255);
        this.status = BillStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelledBy = userUuid;
        registerEvent(new BillCancelled(getUuid(), billNumber, cancelReason));
    }

    /** Printing or re-printing. A cancelled bill can still be printed (it shows as CANCELLED). */
    public void recordPrint() {
        printCount++;
        sharedVia = ShareChannel.PRINT;
    }

    public void recordShare(ShareChannel channel) {
        Guard.isTrue(channel != null, "Share channel is required");
        Guard.isTrue(status == BillStatus.ISSUED, "A cancelled bill cannot be shared");
        if (channel == ShareChannel.PRINT) {
            recordPrint();
        } else {
            sharedVia = channel;
        }
    }

    /** Buyer's state differs from the shop's: IGST instead of CGST + SGST. */
    public boolean isInterState() {
        return placeOfSupply != null && sellerStateCode != null && !placeOfSupply.equals(sellerStateCode);
    }

    public List<BillLine> getLines() {
        return List.copyOf(lines);
    }

    public String amountInWords() {
        return AmountInWords.of(grandTotal);
    }

    // ------------------------------------------------------------------

    private void applySeller(BillSeller s) {
        Guard.isTrue(s != null, "Shop details are required");
        this.sellerName = Guard.requireText(s.name(), "Shop name", 255);
        this.sellerAddress = Guard.trimToNull(s.address());
        this.sellerGstin = Guard.optionalPattern(upper(s.gstin()), "Shop GSTIN", GSTIN, "is not a valid GSTIN");
        this.sellerStateCode = Guard.optionalPattern(s.stateCode(), "Shop state code", STATE_CODE, "must be 2 digits");
        if (sellerStateCode == null && sellerGstin != null) {
            sellerStateCode = sellerGstin.substring(0, 2);
        }
    }

    private void applyBuyer(BillBuyer b) {
        this.customerName = Guard.optionalText(b.name(), "Customer name", 255);
        String phone = Guard.optionalText(b.phone(), "Customer phone", 20);
        this.customerPhone = phone == null ? null
                : Guard.optionalPattern(phone.replace(" ", "").replace("-", ""), "Customer phone", PHONE,
                        "must be 10 to 15 digits");
        this.customerGstin = Guard.optionalPattern(upper(b.gstin()), "Customer GSTIN", GSTIN, "is not a valid GSTIN");
        String place = Guard.optionalPattern(b.placeOfSupply(), "Place of supply", STATE_CODE, "must be a 2-digit state code");
        if (place == null && customerGstin != null) {
            place = customerGstin.substring(0, 2);
        }
        this.placeOfSupply = place == null ? sellerStateCode : place;
    }

    private void applyTotals(BillTotals t) {
        Guard.isTrue(t != null && t.grandTotal() != null, "Bill totals are required");
        this.subTotal = t.subTotal();
        this.discountAmount = t.discountAmount();
        this.taxableAmount = t.taxableAmount();
        this.taxAmount = t.taxAmount();
        this.roundOffAmount = t.roundOffAmount();
        this.grandTotal = t.grandTotal();
    }

    private void checkLinesMatchTotals() {
        Money lineTotals = lines.stream().map(BillLine::getTotalAmount).reduce(Money.ZERO, Money::plus);
        Money lineTax = lines.stream().map(BillLine::getTaxAmount).reduce(Money.ZERO, Money::plus);
        Guard.isTrue(lineTotals.plus(roundOffAmount).equals(grandTotal) && lineTax.equals(taxAmount),
                "Order lines do not add up to the order total; reload the order and try again");
    }

    private static String upper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
