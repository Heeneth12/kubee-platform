package com.kubee.pos.billing.domain;

import com.kubee.pos.common.domain.BaseEntity;
import com.kubee.pos.common.domain.Guard;
import com.kubee.pos.common.domain.Money;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** One printed row of the invoice, with its tax split. Never changes after the bill is issued. */
@Getter
@Entity
@Table(name = "bill_items")
@SQLRestriction("is_deleted = false")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BillLine extends BaseEntity {

    private static final BigDecimal TWO = BigDecimal.valueOf(2);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final Set<TaxType> GST = EnumSet.of(TaxType.CGST, TaxType.SGST, TaxType.IGST);

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @Column(name = "order_item_id")
    private Long orderItemId;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "addons_text", length = 500)
    private String addonsText;

    @Column(name = "hsn_sac_code", length = 20)
    private String hsnSacCode;

    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private String unitOfMeasure;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    /** Including add-ons. */
    @Column(name = "unit_price", nullable = false)
    private Money unitPrice;

    @Column(name = "line_amount", nullable = false)
    private Money lineAmount;

    @Column(name = "discount_amount", nullable = false)
    private Money discountAmount;

    @Column(name = "tax_rate", nullable = false, precision = 6, scale = 3)
    private BigDecimal taxRate;

    @Column(name = "taxable_amount", nullable = false)
    private Money taxableAmount;

    @Column(name = "tax_amount", nullable = false)
    private Money taxAmount;

    @Column(name = "total_amount", nullable = false)
    private Money totalAmount;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "line", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<BillLineTax> taxes = new ArrayList<>();

    BillLine(Bill bill, BillLineSpec spec, boolean interState, int sortOrder) {
        this.bill = bill;
        this.orderItemId = spec.orderItemId();
        this.itemId = spec.itemId();
        this.itemName = Guard.requireText(spec.itemName(), "Item name", 255);
        this.addonsText = Guard.optionalText(spec.addonsText(), "Add-ons", 500);
        this.hsnSacCode = spec.hsnSacCode();
        this.unitOfMeasure = spec.unitOfMeasure() == null ? "PCS" : spec.unitOfMeasure();
        Guard.isTrue(spec.quantity() != null && spec.quantity().signum() > 0, "Quantity must be more than 0");
        this.quantity = spec.quantity();
        this.unitPrice = spec.unitPrice();
        this.lineAmount = spec.lineAmount();
        this.discountAmount = spec.discountAmount();
        this.taxRate = spec.taxRate() == null ? BigDecimal.ZERO : spec.taxRate();
        this.taxableAmount = spec.taxableAmount();
        this.taxAmount = spec.taxAmount();
        this.totalAmount = spec.totalAmount();
        this.sortOrder = sortOrder;
        Guard.isTrue(taxableAmount.plus(taxAmount).equals(totalAmount),
                "Line " + itemName + " does not add up (taxable + tax != total)");
        splitTax(spec.taxComponents(), interState);
    }

    public List<BillLineTax> getTaxes() {
        return List.copyOf(taxes);
    }

    /**
     * Break the line's tax into CGST/SGST (same state) or IGST (other state), plus any CESS.
     * Each part is rounded; the last part takes the leftover paise so the parts add up to the line's tax.
     */
    private void splitTax(List<TaxComponentSpec> groupComponents, boolean interState) {
        if (taxRate.signum() == 0) {
            return;
        }
        List<TaxComponentSpec> parts = components(groupComponents, taxRate, interState);
        Money allocated = Money.ZERO;
        for (int i = 0; i < parts.size(); i++) {
            TaxComponentSpec part = parts.get(i);
            Money amount = i == parts.size() - 1
                    ? taxAmount.minus(allocated)
                    : Money.of(taxableAmount.amount().multiply(part.rate()).divide(HUNDRED, 2, RoundingMode.HALF_UP));
            allocated = allocated.plus(amount);
            taxes.add(new BillLineTax(this, part, taxableAmount, amount));
        }
    }

    static List<TaxComponentSpec> components(List<TaxComponentSpec> groupComponents, BigDecimal rate,
                                             boolean interState) {
        BigDecimal groupRate = groupComponents.stream().map(TaxComponentSpec::rate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // The line keeps the rate it was sold at; if the group was changed since, fall back to a plain GST split.
        List<TaxComponentSpec> base = !groupComponents.isEmpty() && groupRate.compareTo(rate) == 0
                ? groupComponents
                : List.of(new TaxComponentSpec(null, TaxType.IGST, "IGST", rate));

        List<TaxComponentSpec> gst = base.stream().filter(c -> GST.contains(c.type())).toList();
        List<TaxComponentSpec> others = base.stream().filter(c -> !GST.contains(c.type())).toList();
        BigDecimal gstRate = gst.stream().map(TaxComponentSpec::rate).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TaxComponentSpec> result = new ArrayList<>();
        if (gstRate.signum() > 0) {
            if (interState) {
                Long id = gst.size() == 1 && gst.getFirst().type() == TaxType.IGST ? gst.getFirst().id() : null;
                result.add(new TaxComponentSpec(id, TaxType.IGST, "IGST", gstRate));
            } else if (gst.stream().allMatch(c -> c.type() != TaxType.IGST)) {
                result.addAll(gst);
            } else {
                BigDecimal half = gstRate.divide(TWO, 3, RoundingMode.HALF_UP);
                result.add(new TaxComponentSpec(null, TaxType.CGST, "CGST", half));
                result.add(new TaxComponentSpec(null, TaxType.SGST, "SGST", gstRate.subtract(half)));
            }
        }
        result.addAll(others);
        return result;
    }
}
