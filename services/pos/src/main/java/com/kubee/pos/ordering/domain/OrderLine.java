package com.kubee.pos.ordering.domain;

import com.kubee.pos.common.domain.BaseEntity;
import com.kubee.pos.common.domain.Guard;
import com.kubee.pos.common.domain.Money;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * One row of the order: "2 x Veg Maggi (Full) + Extra Cheese". Name, price and tax are snapshots.
 * Only changed through its {@link Order}, which recomputes the money columns after every change.
 */
@Getter
@Entity
@Table(name = "order_items")
@SQLRestriction("is_deleted = false")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderLine extends BaseEntity {

    private static final BigDecimal MAX_QUANTITY = BigDecimal.valueOf(100_000);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "item_variant_id")
    private Long variantId;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "variant_name", length = 100)
    private String variantName;

    @Column(name = "hsn_sac_code", length = 20)
    private String hsnSacCode;

    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private String unitOfMeasure;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false)
    private Money unitPrice;

    @Column(name = "addons_unit_price", nullable = false)
    private Money addonsUnitPrice;

    @Column(name = "price_includes_tax", nullable = false)
    private boolean priceIncludesTax;

    @Column(name = "line_amount", nullable = false)
    private Money lineAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 10)
    private DiscountType discountType;

    @Column(name = "discount_value", precision = 18, scale = 2)
    private BigDecimal discountValue;

    /** Line discount + this line's share of the order discount. */
    @Column(name = "discount_amount", nullable = false)
    private Money discountAmount;

    @Column(name = "tax_group_id")
    private Long taxGroupId;

    @Column(name = "tax_rate", nullable = false, precision = 6, scale = 3)
    private BigDecimal taxRate;

    @Column(name = "taxable_amount", nullable = false)
    private Money taxableAmount;

    @Column(name = "tax_amount", nullable = false)
    private Money taxAmount;

    @Column(name = "total_amount", nullable = false)
    private Money totalAmount;

    @Column(name = "notes")
    private String notes;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "line", cascade = CascadeType.ALL)
    @SQLRestriction("is_deleted = false")
    @OrderBy("id ASC")
    private List<OrderLineAddon> addons = new ArrayList<>();

    OrderLine(Order order, LineSpec spec, int sortOrder) {
        Guard.isTrue(spec.unitPrice() != null && !spec.unitPrice().isNegative(), "Price cannot be negative");
        Guard.isTrue(spec.taxRate().signum() >= 0, "Tax rate cannot be negative");
        this.order = order;
        this.itemId = spec.itemId();
        this.variantId = spec.variantId();
        this.itemName = Guard.requireText(spec.itemName(), "Item name", 255);
        this.variantName = Guard.optionalText(spec.variantName(), "Variant name", 100);
        this.hsnSacCode = Guard.optionalText(spec.hsnSacCode(), "HSN/SAC code", 20);
        String unit = Guard.optionalText(spec.unitOfMeasure(), "Unit of measure", 20);
        this.unitOfMeasure = unit == null ? "PCS" : unit.toUpperCase(Locale.ROOT);
        this.unitPrice = spec.unitPrice();
        this.priceIncludesTax = spec.priceIncludesTax();
        this.taxGroupId = spec.taxGroupId();
        this.taxRate = spec.taxRate();
        this.sortOrder = sortOrder;
        spec.addons().forEach(a -> addons.add(new OrderLineAddon(this, a)));
        this.addonsUnitPrice = getAddons().stream().map(OrderLineAddon::perUnitPrice).reduce(Money.ZERO, Money::plus);
        change(spec.quantity(), spec.discount(), spec.notes());
        applyTotals(Money.ZERO);
    }

    public List<OrderLineAddon> getAddons() {
        return addons.stream().filter(a -> !a.isDeleted()).toList();
    }

    public Discount getDiscount() {
        return Discount.ofNullable(discountType, discountValue);
    }

    /** Quantity, line discount and kitchen note are what the cashier can change on an existing line. */
    void change(BigDecimal quantity, Discount discount, String notes) {
        this.quantity = checkQuantity(quantity);
        this.lineAmount = unitPrice.plus(addonsUnitPrice).times(this.quantity);
        if (discount != null) {
            discount.checkFits(lineAmount, "line amount");
        }
        this.discountType = discount == null ? null : discount.type();
        this.discountValue = discount == null ? null : discount.value();
        this.notes = Guard.optionalText(notes, "Line note", 255);
    }

    void addQuantity(BigDecimal more) {
        change(quantity.add(checkQuantity(more)), getDiscount(), notes);
    }

    /** Same item, variant, price and add-ons, with no discount or note: a repeat scan just bumps the quantity. */
    boolean canAbsorb(LineSpec spec) {
        if (itemId == null || !itemId.equals(spec.itemId()) || !Objects.equals(variantId, spec.variantId())
                || unitPrice.compareTo(spec.unitPrice()) != 0
                || discountType != null || spec.discount() != null
                || notes != null || Guard.trimToNull(spec.notes()) != null) {
            return false;
        }
        List<OrderLineAddon> mine = getAddons();
        return mine.size() == spec.addons().size()
                && spec.addons().stream().allMatch(a -> mine.stream().anyMatch(m -> m.sameAs(a)));
    }

    Money lineDiscount() {
        Discount discount = getDiscount();
        return discount == null ? Money.ZERO : discount.amountOf(lineAmount);
    }

    /** What the order discount is spread over: the line amount after its own discount. */
    Money amountAfterLineDiscount() {
        return lineAmount.minus(lineDiscount());
    }

    /** Recompute tax and totals, given this line's share of the order-level discount. */
    void applyTotals(Money orderDiscountShare) {
        this.discountAmount = lineDiscount().plus(orderDiscountShare);
        Money net = lineAmount.minus(discountAmount);
        if (taxRate.signum() == 0) {
            this.taxableAmount = net;
            this.taxAmount = Money.ZERO;
        } else if (priceIncludesTax) {
            this.taxableAmount = Money.of(net.amount().multiply(HUNDRED)
                    .divide(HUNDRED.add(taxRate), 2, RoundingMode.HALF_UP));
            this.taxAmount = net.minus(taxableAmount);
        } else {
            this.taxableAmount = net;
            this.taxAmount = Money.of(net.amount().multiply(taxRate).divide(HUNDRED, 2, RoundingMode.HALF_UP));
        }
        this.totalAmount = taxableAmount.plus(taxAmount);
    }

    void remove() {
        addons.forEach(OrderLineAddon::remove);
        markDeleted();
    }

    private static BigDecimal checkQuantity(BigDecimal quantity) {
        Guard.isTrue(quantity != null && quantity.signum() > 0, "Quantity must be more than 0");
        Guard.isTrue(quantity.compareTo(MAX_QUANTITY) <= 0, "Quantity is too large");
        Guard.isTrue(quantity.stripTrailingZeros().scale() <= 3, "Quantity can have at most 3 decimals");
        return quantity.setScale(3, RoundingMode.UNNECESSARY);
    }
}
