package com.kubee.pos.ordering.domain;

import com.kubee.pos.common.domain.AggregateRoot;
import com.kubee.pos.common.domain.DomainException;
import com.kubee.pos.common.domain.Guard;
import com.kubee.pos.common.domain.Money;
import com.kubee.pos.ordering.domain.event.OrderCancelled;
import com.kubee.pos.ordering.domain.event.OrderCompleted;
import com.kubee.pos.ordering.domain.event.PaymentReceived;
import com.kubee.pos.ordering.domain.event.PaymentRefunded;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * A customer's order at the counter, with its lines and the money taken against it. Online orders
 * (Zomato / Swiggy) are entered the same way with their {@link OrderSource} and paid with
 * {@link PaymentMethod#AGGREGATOR}.
 * <p>
 * Lifecycle: {@code OPEN} (being billed) ⇄ {@code HELD} (parked, recall later) → {@code COMPLETED}
 * (fully paid, closes itself on the last payment) or {@code CANCELLED} (only once nothing is paid).
 * Lines, discounts and payments can only change while {@code OPEN}; refunds work on any order that
 * is not cancelled. Every change recomputes discounts, GST, round-off and the payment status.
 * <p>
 * JPA entity name is {@code PosOrder} because {@code Order} clashes with the ORDER keyword in JPQL.
 */
@Getter
@Entity(name = "PosOrder")
@Table(name = "orders")
@SQLRestriction("is_deleted = false")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends AggregateRoot {

    private static final Pattern PHONE = Pattern.compile("\\+?\\d{10,15}");

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "client_ref", length = 64)
    private String clientRef;

    /** Token shown to the customer, restarts every day: "23". */
    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private OrderSource source;

    /** Zomato / Swiggy order id, so the shop can match the order with the aggregator's payout. */
    @Column(name = "external_order_id", length = 64)
    private String externalOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "table_label", length = 50)
    private String tableLabel;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    /** Sum of line amounts before any discount. */
    @Column(name = "sub_total", nullable = false)
    private Money subTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 10)
    private DiscountType discountType;

    @Column(name = "discount_value", precision = 18, scale = 2)
    private BigDecimal discountValue;

    /** Line discounts + order discount, in rupees. */
    @Column(name = "discount_amount", nullable = false)
    private Money discountAmount;

    @Column(name = "discount_reason")
    private String discountReason;

    @Column(name = "taxable_amount", nullable = false)
    private Money taxableAmount;

    @Column(name = "tax_amount", nullable = false)
    private Money taxAmount;

    @Column(name = "round_off_amount", nullable = false)
    private Money roundOffAmount;

    @Column(name = "grand_total", nullable = false)
    private Money grandTotal;

    /** Payments minus refunds. */
    @Column(name = "paid_amount", nullable = false)
    private Money paidAmount;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by", length = 36)
    private String cancelledBy;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @SQLRestriction("is_deleted = false")
    @OrderBy("sortOrder ASC, id ASC")
    private List<OrderLine> lines = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @SQLRestriction("is_deleted = false")
    @OrderBy("paidAt ASC, id ASC")
    private List<Payment> payments = new ArrayList<>();

    public static Order open(String orderNumber, String clientRef, CustomerDetails details, String createdBy) {
        Order order = new Order();
        order.orderNumber = Guard.requireText(orderNumber, "Order number", 50);
        order.clientRef = Guard.optionalText(clientRef, "Client reference", 64);
        order.status = OrderStatus.OPEN;
        order.paymentStatus = PaymentStatus.UNPAID;
        order.createdBy = createdBy;
        order.paidAmount = Money.ZERO;
        order.applyDetails(details == null ? CustomerDetails.NONE : details);
        order.recalculate(RoundOffMode.NONE);
        return order;
    }

    // ---------------------------------------------------------------- details

    /** Customer, order type, table and notes. Allowed until the order is cancelled (e.g. phone for the e-bill). */
    public void updateDetails(CustomerDetails details) {
        requireNotCancelled();
        applyDetails(details);
    }

    // ---------------------------------------------------------------- lines

    /** Adds a line, or bumps the quantity of an identical plain line (repeat scan). */
    public OrderLine addLine(LineSpec spec, RoundOffMode roundOff) {
        requireOpen();
        OrderLine line = getLines().stream()
                .filter(existing -> existing.canAbsorb(spec))
                .findFirst()
                .orElse(null);
        if (line == null) {
            line = new OrderLine(this, spec, lines.size());
            lines.add(line);
        } else {
            line.addQuantity(spec.quantity());
        }
        recalculate(roundOff);
        return line;
    }

    public void changeLine(String lineUuid, BigDecimal quantity, Discount discount, String notes, RoundOffMode roundOff) {
        requireOpen();
        liveLine(lineUuid).change(quantity, discount, notes);
        recalculate(roundOff);
    }

    public void removeLine(String lineUuid, RoundOffMode roundOff) {
        requireOpen();
        liveLine(lineUuid).remove();
        recalculate(roundOff);
    }

    /** Bill-level discount on top of line discounts; {@code null} removes it. */
    public void applyDiscount(Discount discount, String reason, RoundOffMode roundOff) {
        requireOpen();
        if (discount != null) {
            discount.checkFits(amountAfterLineDiscounts(), "order amount");
        }
        this.discountType = discount == null ? null : discount.type();
        this.discountValue = discount == null ? null : discount.value();
        this.discountReason = discount == null ? null : Guard.optionalText(reason, "Discount reason", 255);
        recalculate(roundOff);
    }

    public List<OrderLine> getLines() {
        return lines.stream().filter(l -> !l.isDeleted()).toList();
    }

    public Discount getDiscount() {
        return Discount.ofNullable(discountType, discountValue);
    }

    // ---------------------------------------------------------------- lifecycle

    /** Park the order (customer went to fetch one more item). */
    public void hold() {
        requireOpen();
        requireLines("holding");
        status = OrderStatus.HELD;
    }

    public void recall() {
        Guard.isTrue(status == OrderStatus.HELD, "Only a held order can be recalled (this one is " + status + ")");
        status = OrderStatus.OPEN;
    }

    /** Close an order with nothing left to pay. Orders also complete themselves on the last payment. */
    public void complete() {
        requireOpen();
        requireLines("completing");
        Guard.isTrue(dueAmount().isZero(), dueAmount() + " is still due on this order");
        markCompleted();
    }

    public void cancel(String reason, String userUuid) {
        requireNotCancelled();
        Guard.isTrue(paidAmount.isZero(),
                paidAmount + " has been paid on this order. Refund it before cancelling.");
        this.cancelReason = Guard.requireText(reason, "Cancel reason", 255);
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelledBy = userUuid;
        registerEvent(new OrderCancelled(getUuid(), cancelReason));
    }

    // ---------------------------------------------------------------- payments

    /**
     * Take money against the order. Split payment = call this once per method.
     * The order completes itself when nothing is left to pay.
     */
    public Payment recordPayment(PaymentSpec spec, String userUuid) {
        Guard.isTrue(status == OrderStatus.OPEN, status == OrderStatus.HELD
                ? "Recall the held order before taking payment"
                : "Payment can only be taken on an open order (this one is " + status + ")");
        requireLines("taking payment");
        Guard.isTrue(spec.method() != null, "Payment method is required");
        Guard.isTrue(spec.method() != PaymentMethod.AGGREGATOR || source.isOnline(),
                "Aggregator payment is only for Zomato / Swiggy orders");
        Money due = dueAmount();
        Guard.isTrue(!due.isZero(), "Nothing is due on this order");

        Money amount = spec.amount();
        Money tendered = spec.tenderedAmount();
        if (spec.method() == PaymentMethod.CASH) {
            if (amount == null) {
                Guard.isTrue(tendered != null, "Amount or tendered amount is required");
                amount = tendered.isGreaterThan(due) ? due : tendered;
            }
            Guard.isTrue(tendered == null || !amount.isGreaterThan(tendered),
                    "Tendered amount cannot be less than the amount paid");
        } else {
            Guard.isTrue(tendered == null, "Tendered amount is only for cash payments");
            Guard.isTrue(amount != null, "Amount is required");
        }
        Guard.isTrue(amount.amount().signum() > 0, "Amount must be more than 0");
        Guard.isTrue(!amount.isGreaterThan(due), "Amount " + amount + " is more than the " + due + " due");

        Payment payment = Payment.payment(this, spec, amount, tendered, userUuid);
        payments.add(payment);
        paidAmount = paidAmount.plus(amount);
        refreshPaymentStatus();
        registerEvent(new PaymentReceived(getUuid(), payment.getUuid(), payment.getPaymentMethod(), amount));
        if (dueAmount().isZero()) {
            markCompleted();
        }
        return payment;
    }

    /** Give money back against one payment (by default through the same method). */
    public Payment refund(String paymentUuid, Money amount, PaymentMethod method, String reason,
                          String clientRef, String userUuid) {
        requireNotCancelled();
        Payment original = getPayments().stream()
                .filter(p -> p.getUuid().equals(paymentUuid))
                .findFirst()
                .orElseThrow(() -> new DomainException("Payment " + paymentUuid + " is not part of this order"));
        Guard.isTrue(original.getPaymentType() == PaymentType.PAYMENT && original.isSuccessful(),
                "Only a successful payment can be refunded");
        Guard.isTrue(method != PaymentMethod.AGGREGATOR || source.isOnline(),
                "Aggregator refund is only for Zomato / Swiggy orders");
        Money refundable = refundableAmount(original);
        Guard.isTrue(amount != null && amount.amount().signum() > 0, "Refund amount must be more than 0");
        Guard.isTrue(!amount.isGreaterThan(refundable),
                "Refund " + amount + " is more than the " + refundable + " left to refund on this payment");

        Payment refund = Payment.refund(this, original, amount, method, reason, clientRef, userUuid);
        payments.add(refund);
        paidAmount = paidAmount.minus(amount);
        refreshPaymentStatus();
        registerEvent(new PaymentRefunded(getUuid(), refund.getUuid(), original.getUuid(),
                refund.getPaymentMethod(), amount));
        return refund;
    }

    public List<Payment> getPayments() {
        return payments.stream().filter(p -> !p.isDeleted()).toList();
    }

    public Optional<Payment> findPaymentByClientRef(String clientRef) {
        String ref = Guard.trimToNull(clientRef);
        return ref == null ? Optional.empty()
                : getPayments().stream().filter(p -> ref.equals(p.getClientRef())).findFirst();
    }

    /** What the customer still owes. Zero once completed or cancelled (refunded money is not "due"). */
    public Money dueAmount() {
        if (status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) {
            return Money.ZERO;
        }
        Money due = grandTotal.minus(paidAmount);
        return due.isNegative() ? Money.ZERO : due;
    }

    public Money refundableAmount(Payment payment) {
        Money refunded = getPayments().stream()
                .filter(p -> p.isSuccessful() && p.isRefundOf(payment))
                .map(Payment::getAmount)
                .reduce(Money.ZERO, Money::plus);
        return payment.getAmount().minus(refunded);
    }

    // ---------------------------------------------------------------- internals

    private void applyDetails(CustomerDetails d) {
        this.orderType = d.orderType() == null ? (orderType == null ? OrderType.COUNTER : orderType) : d.orderType();
        this.source = d.source() == null ? (source == null ? OrderSource.POS : source) : d.source();
        this.externalOrderId = Guard.optionalText(d.externalOrderId(), "Online order id", 64);
        Guard.isTrue(externalOrderId == null || source.isOnline(), "Online order id is only for Zomato / Swiggy orders");
        Guard.isTrue(source.isOnline() || getPayments().stream().noneMatch(p -> p.getPaymentMethod() == PaymentMethod.AGGREGATOR),
                "This order was paid through the aggregator, so it must stay an online order");
        this.customerName = Guard.optionalText(d.customerName(), "Customer name", 255);
        String phone = Guard.optionalText(d.customerPhone(), "Customer phone", 20);
        this.customerPhone = phone == null ? null
                : Guard.optionalPattern(phone.replace(" ", "").replace("-", ""), "Customer phone", PHONE,
                        "must be 10 to 15 digits");
        this.tableLabel = Guard.optionalText(d.tableLabel(), "Table", 50);
        this.notes = Guard.trimToNull(d.notes());
    }

    /**
     * Totals, in order: line amounts → line discounts → order discount spread over lines by value
     * (the biggest line takes the leftover paise) → GST per line (inclusive or exclusive) → round-off.
     */
    private void recalculate(RoundOffMode roundOff) {
        List<OrderLine> live = getLines();
        Money afterLineDiscounts = amountAfterLineDiscounts();
        Discount discount = getDiscount();
        Money orderDiscount = discount == null ? Money.ZERO : discount.amountOf(afterLineDiscounts);

        Money[] shares = splitByValue(orderDiscount, live, afterLineDiscounts);
        for (int i = 0; i < live.size(); i++) {
            live.get(i).applyTotals(shares[i]);
        }

        subTotal = sum(live, OrderLine::getLineAmount);
        discountAmount = sum(live, OrderLine::getDiscountAmount);
        taxableAmount = sum(live, OrderLine::getTaxableAmount);
        taxAmount = sum(live, OrderLine::getTaxAmount);
        Money beforeRoundOff = sum(live, OrderLine::getTotalAmount);
        roundOffAmount = Objects.requireNonNullElse(roundOff, RoundOffMode.NONE).adjustmentFor(beforeRoundOff);
        grandTotal = beforeRoundOff.plus(roundOffAmount);

        Guard.isTrue(!paidAmount.isGreaterThan(grandTotal),
                "Order total " + grandTotal + " cannot be less than the " + paidAmount + " already paid. Refund first.");
        refreshPaymentStatus();
    }

    /** Spread {@code total} over the lines in proportion to their value; rounding paise go to the biggest line. */
    private static Money[] splitByValue(Money total, List<OrderLine> lines, Money base) {
        Money[] shares = new Money[lines.size()];
        Money allocated = Money.ZERO;
        int biggest = -1;
        for (int i = 0; i < lines.size(); i++) {
            Money value = lines.get(i).amountAfterLineDiscount();
            shares[i] = base.isZero() ? Money.ZERO : Money.of(total.amount().multiply(value.amount())
                    .divide(base.amount(), 2, RoundingMode.HALF_UP));
            allocated = allocated.plus(shares[i]);
            if (biggest < 0 || value.isGreaterThan(lines.get(biggest).amountAfterLineDiscount())) {
                biggest = i;
            }
        }
        if (biggest >= 0) {
            shares[biggest] = shares[biggest].plus(total.minus(allocated));
        }
        return shares;
    }

    private Money amountAfterLineDiscounts() {
        return sum(getLines(), OrderLine::amountAfterLineDiscount);
    }

    private void refreshPaymentStatus() {
        boolean anyRefund = getPayments().stream()
                .anyMatch(p -> p.isSuccessful() && p.getPaymentType() == PaymentType.REFUND);
        if (anyRefund) {
            paymentStatus = paidAmount.isZero() ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
        } else if (paidAmount.isZero()) {
            paymentStatus = status == OrderStatus.COMPLETED ? PaymentStatus.PAID : PaymentStatus.UNPAID;
        } else {
            paymentStatus = paidAmount.compareTo(grandTotal) >= 0 ? PaymentStatus.PAID : PaymentStatus.PARTIALLY_PAID;
        }
    }

    private void markCompleted() {
        status = OrderStatus.COMPLETED;
        completedAt = LocalDateTime.now();
        refreshPaymentStatus();
        registerEvent(new OrderCompleted(getUuid()));
    }

    private OrderLine liveLine(String lineUuid) {
        return getLines().stream()
                .filter(l -> l.getUuid().equals(lineUuid))
                .findFirst()
                .orElseThrow(() -> new DomainException("Line " + lineUuid + " is not part of this order"));
    }

    private void requireOpen() {
        Guard.isTrue(status == OrderStatus.OPEN, status == OrderStatus.HELD
                ? "This order is on hold. Recall it before changing it."
                : "Only an open order can be changed (this one is " + status + ")");
    }

    private void requireNotCancelled() {
        Guard.isTrue(status != OrderStatus.CANCELLED, "This order is cancelled");
    }

    private void requireLines(String action) {
        Guard.isTrue(!getLines().isEmpty(), "Add at least one item before " + action);
    }

    private static Money sum(List<OrderLine> lines, Function<OrderLine, Money> amount) {
        return lines.stream().map(amount).reduce(Money.ZERO, Money::plus);
    }
}
