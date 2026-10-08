package com.kubee.pos.ordering.domain;

import com.kubee.pos.common.domain.DomainException;
import com.kubee.pos.common.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static final RoundOffMode NO_ROUND = RoundOffMode.NONE;

    private static Order newOrder() {
        return Order.open("1", null, CustomerDetails.NONE, "user-1");
    }

    /** Untaxed, tax-inclusive item line. */
    private static LineSpec line(long itemId, String price, String qty) {
        return line(itemId, price, qty, "0", true, List.of(), null);
    }

    private static LineSpec line(long itemId, String price, String qty, String taxRate, boolean inclusive,
                                 List<LineAddonSpec> addons, Discount discount) {
        return new LineSpec(itemId, null, "Item " + itemId, null, null, "PCS", new BigDecimal(qty), Money.of(price),
                inclusive, null, new BigDecimal(taxRate), addons, discount, null);
    }

    private static PaymentSpec pay(PaymentMethod method, String amount, String tendered) {
        return new PaymentSpec(method, amount == null ? null : Money.of(amount),
                tendered == null ? null : Money.of(tendered), null, null, null);
    }

    // ---------------------------------------------------------------- totals and GST

    @Test
    void opensEmptyCounterOrder() {
        Order order = newOrder();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(order.getOrderType()).isEqualTo(OrderType.COUNTER);
        assertThat(order.getGrandTotal()).isEqualTo(Money.ZERO);
    }

    @Test
    void splitsTaxOutOfInclusivePrice() {
        Order order = newOrder();
        order.addLine(line(1, "105", "1", "5", true, List.of(), null), NO_ROUND);

        assertThat(order.getTaxableAmount()).isEqualTo(Money.of("100"));
        assertThat(order.getTaxAmount()).isEqualTo(Money.of("5"));
        assertThat(order.getGrandTotal()).isEqualTo(Money.of("105"));
    }

    @Test
    void addsTaxOnTopOfExclusivePrice() {
        Order order = newOrder();
        order.addLine(line(1, "100", "2", "18", false, List.of(), null), NO_ROUND);

        assertThat(order.getSubTotal()).isEqualTo(Money.of("200"));
        assertThat(order.getTaxAmount()).isEqualTo(Money.of("36"));
        assertThat(order.getGrandTotal()).isEqualTo(Money.of("236"));
    }

    @Test
    void addOnsArePricedPerUnitOfTheLine() {
        var cheese = new LineAddonSpec(9L, "Extra Cheese", BigDecimal.ONE, Money.of("20"));
        Order order = newOrder();
        OrderLine line = order.addLine(line(1, "70", "2", "0", true, List.of(cheese), null), NO_ROUND);

        assertThat(line.getAddonsUnitPrice()).isEqualTo(Money.of("20"));
        assertThat(line.getLineAmount()).isEqualTo(Money.of("180"));
    }

    @Test
    void repeatScanBumpsQuantityButDifferentAddonsMakeANewLine() {
        var cheese = new LineAddonSpec(9L, "Extra Cheese", BigDecimal.ONE, Money.of("20"));
        Order order = newOrder();
        order.addLine(line(1, "70", "1"), NO_ROUND);
        order.addLine(line(1, "70", "1"), NO_ROUND);
        order.addLine(line(1, "70", "1", "0", true, List.of(cheese), null), NO_ROUND);

        assertThat(order.getLines()).hasSize(2);
        assertThat(order.getLines().getFirst().getQuantity()).isEqualByComparingTo("2");
    }

    @Test
    void changesAndRemovesLines() {
        Order order = newOrder();
        OrderLine tea = order.addLine(line(1, "20", "1"), NO_ROUND);
        OrderLine samosa = order.addLine(line(2, "15", "1"), NO_ROUND);

        order.changeLine(tea.getUuid(), new BigDecimal("3"), null, "less sugar", NO_ROUND);
        order.removeLine(samosa.getUuid(), NO_ROUND);

        assertThat(order.getLines()).hasSize(1);
        assertThat(order.getGrandTotal()).isEqualTo(Money.of("60"));
        assertThat(order.getLines().getFirst().getNotes()).isEqualTo("less sugar");
    }

    @Test
    void rejectsBadQuantity() {
        Order order = newOrder();
        assertThatThrownBy(() -> order.addLine(line(1, "10", "0"), NO_ROUND)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> order.addLine(line(1, "10", "1.2345"), NO_ROUND)).isInstanceOf(DomainException.class);
    }

    // ---------------------------------------------------------------- discounts and round-off

    @Test
    void lineDiscountCannotExceedTheLine() {
        Order order = newOrder();
        var tooMuch = new Discount(DiscountType.FLAT, new BigDecimal("150"));

        assertThatThrownBy(() -> order.addLine(line(1, "100", "1", "0", true, List.of(), tooMuch), NO_ROUND))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("line amount");
    }

    @Test
    void orderDiscountIsSpreadOverLinesByValue() {
        Order order = newOrder();
        OrderLine big = order.addLine(line(1, "100", "1"), NO_ROUND);
        OrderLine small = order.addLine(line(2, "50", "1"), NO_ROUND);

        order.applyDiscount(new Discount(DiscountType.PERCENT, BigDecimal.TEN), "regular", NO_ROUND);

        assertThat(big.getDiscountAmount()).isEqualTo(Money.of("10"));
        assertThat(small.getDiscountAmount()).isEqualTo(Money.of("5"));
        assertThat(order.getDiscountAmount()).isEqualTo(Money.of("15"));
        assertThat(order.getGrandTotal()).isEqualTo(Money.of("135"));
    }

    @Test
    void flatDiscountLeftoverPaiseStayExact() {
        Order order = newOrder();
        order.addLine(line(1, "10", "1"), NO_ROUND);
        order.addLine(line(2, "10", "1"), NO_ROUND);
        order.addLine(line(3, "10", "1"), NO_ROUND);

        order.applyDiscount(new Discount(DiscountType.FLAT, BigDecimal.TEN), null, NO_ROUND);

        assertThat(order.getDiscountAmount()).isEqualTo(Money.of("10"));
        assertThat(order.getGrandTotal()).isEqualTo(Money.of("20"));
        order.applyDiscount(null, null, NO_ROUND);
        assertThat(order.getGrandTotal()).isEqualTo(Money.of("30"));
    }

    @Test
    void lineAndOrderDiscountsStack() {
        Order order = newOrder();
        var lineOff = new Discount(DiscountType.FLAT, new BigDecimal("20"));
        order.addLine(line(1, "100", "1", "0", true, List.of(), lineOff), NO_ROUND);

        order.applyDiscount(new Discount(DiscountType.PERCENT, BigDecimal.TEN), null, NO_ROUND);

        assertThat(order.getDiscountAmount()).isEqualTo(Money.of("28")); // 20 + 10% of 80
        assertThat(order.getGrandTotal()).isEqualTo(Money.of("72"));
    }

    @Test
    void roundOffModes() {
        assertThat(RoundOffMode.NEAREST_1.adjustmentFor(Money.of("99.50"))).isEqualTo(Money.of("0.50"));
        assertThat(RoundOffMode.NEAREST_1.adjustmentFor(Money.of("99.49"))).isEqualTo(Money.of("-0.49"));
        assertThat(RoundOffMode.NEAREST_0_50.adjustmentFor(Money.of("99.30"))).isEqualTo(Money.of("0.20"));
        assertThat(RoundOffMode.DOWN_1.adjustmentFor(Money.of("99.99"))).isEqualTo(Money.of("-0.99"));
        assertThat(RoundOffMode.NONE.adjustmentFor(Money.of("99.99"))).isEqualTo(Money.ZERO);
    }

    @Test
    void grandTotalIsRounded() {
        Order order = newOrder();
        order.addLine(line(1, "100", "1", "5", false, List.of(), null), RoundOffMode.NEAREST_1); // 105
        order.addLine(line(2, "0.40", "1"), RoundOffMode.NEAREST_1);                              // 105.40

        assertThat(order.getRoundOffAmount()).isEqualTo(Money.of("-0.40"));
        assertThat(order.getGrandTotal()).isEqualTo(Money.of("105"));
    }

    // ---------------------------------------------------------------- hold / recall / cancel

    @Test
    void heldOrderCannotBeChangedUntilRecalled() {
        Order order = newOrder();
        order.addLine(line(1, "10", "1"), NO_ROUND);
        order.hold();

        assertThatThrownBy(() -> order.addLine(line(2, "10", "1"), NO_ROUND))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Recall");
        assertThatThrownBy(() -> order.recordPayment(pay(PaymentMethod.UPI, "10", null), null))
                .isInstanceOf(DomainException.class);

        order.recall();
        order.addLine(line(2, "10", "1"), NO_ROUND);
        assertThat(order.getGrandTotal()).isEqualTo(Money.of("20"));
    }

    @Test
    void emptyOrderCannotBeHeldOrPaid() {
        Order order = newOrder();
        assertThatThrownBy(order::hold).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> order.recordPayment(pay(PaymentMethod.CASH, "10", null), null))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void cancelNeedsReasonAndNothingPaid() {
        Order order = newOrder();
        order.addLine(line(1, "100", "1"), NO_ROUND);
        order.recordPayment(pay(PaymentMethod.UPI, "40", null), null);

        assertThatThrownBy(() -> order.cancel("customer left", "u")).hasMessageContaining("Refund");
        assertThatThrownBy(() -> order.cancel(" ", "u")).isInstanceOf(DomainException.class);

        Payment upi = order.getPayments().getFirst();
        order.refund(upi.getUuid(), Money.of("40"), null, "customer left", null, "u");
        order.cancel("customer left", "u");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(order.getCancelledBy()).isEqualTo("u");
        assertThatThrownBy(() -> order.addLine(line(2, "10", "1"), NO_ROUND)).isInstanceOf(DomainException.class);
    }

    // ---------------------------------------------------------------- payments

    @Test
    void cashWithTenderedGivesChangeAndCompletesTheOrder() {
        Order order = newOrder();
        order.addLine(line(1, "380", "1"), NO_ROUND);

        Payment cash = order.recordPayment(pay(PaymentMethod.CASH, null, "500"), "u");

        assertThat(cash.getAmount()).isEqualTo(Money.of("380"));
        assertThat(cash.getChangeAmount()).isEqualTo(Money.of("120"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order.getCompletedAt()).isNotNull();
        assertThat(order.domainEvents()).extracting(e -> e.getClass().getSimpleName())
                .contains("PaymentReceived", "OrderCompleted");
    }

    @Test
    void splitPaymentCompletesOnTheLastPart() {
        Order order = newOrder();
        order.addLine(line(1, "300", "1"), NO_ROUND);

        order.recordPayment(pay(PaymentMethod.UPI, "200", null), null);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
        assertThat(order.dueAmount()).isEqualTo(Money.of("100"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.OPEN);

        order.recordPayment(pay(PaymentMethod.CASH, "100", null), null);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getPayments()).hasSize(2);
    }

    @Test
    void rejectsOverpaymentAndTenderedOnNonCash() {
        Order order = newOrder();
        order.addLine(line(1, "100", "1"), NO_ROUND);

        assertThatThrownBy(() -> order.recordPayment(pay(PaymentMethod.UPI, "150", null), null))
                .hasMessageContaining("more than");
        assertThatThrownBy(() -> order.recordPayment(pay(PaymentMethod.UPI, "100", "100"), null))
                .hasMessageContaining("only for cash");
        assertThatThrownBy(() -> order.recordPayment(pay(PaymentMethod.CASH, "100", "50"), null))
                .hasMessageContaining("Tendered");
    }

    @Test
    void totalCannotDropBelowWhatIsPaid() {
        Order order = newOrder();
        OrderLine tea = order.addLine(line(1, "50", "1"), NO_ROUND);
        order.addLine(line(2, "50", "1"), NO_ROUND);
        order.recordPayment(pay(PaymentMethod.UPI, "80", null), null);

        assertThatThrownBy(() -> order.removeLine(tea.getUuid(), NO_ROUND)).hasMessageContaining("already paid");
    }

    @Test
    void partialRefundOnACompletedOrder() {
        Order order = newOrder();
        order.addLine(line(1, "100", "1"), NO_ROUND);
        Payment upi = order.recordPayment(pay(PaymentMethod.UPI, "100", null), null);

        Payment refund = order.refund(upi.getUuid(), Money.of("30"), PaymentMethod.CASH, "cold coffee", null, "u");

        assertThat(refund.getPaymentType()).isEqualTo(PaymentType.REFUND);
        assertThat(refund.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(order.getPaidAmount()).isEqualTo(Money.of("70"));
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.dueAmount()).isEqualTo(Money.ZERO);
        assertThat(order.refundableAmount(upi)).isEqualTo(Money.of("70"));
        assertThatThrownBy(() -> order.refund(upi.getUuid(), Money.of("71"), null, "again", null, "u"))
                .hasMessageContaining("left to refund");
        assertThatThrownBy(() -> order.refund(refund.getUuid(), Money.of("1"), null, "refund a refund", null, "u"))
                .hasMessageContaining("Only a successful payment");
    }

    @Test
    void completeNeedsNothingDue() {
        Order order = newOrder();
        order.addLine(line(1, "10", "1"), NO_ROUND);
        assertThatThrownBy(order::complete).hasMessageContaining("still due");

        Order free = newOrder();
        free.addLine(line(1, "0", "1"), NO_ROUND);
        free.complete();
        assertThat(free.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(free.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void validatesCustomerPhone() {
        Order order = newOrder();
        order.updateDetails(new CustomerDetails(OrderType.TAKEAWAY, "Ravi", "98480 22338", null, null));
        assertThat(order.getCustomerPhone()).isEqualTo("9848022338");
        assertThat(order.getOrderType()).isEqualTo(OrderType.TAKEAWAY);

        assertThatThrownBy(() -> order.updateDetails(new CustomerDetails(null, null, "12345", null, null)))
                .hasMessageContaining("10 to 15 digits");
    }

    // ---------------------------------------------------------------- online orders

    private static CustomerDetails online(OrderSource source, String externalOrderId) {
        return new CustomerDetails(OrderType.DELIVERY, null, null, null, null, source, externalOrderId);
    }

    @Test
    void counterOrderIsFromThePos() {
        assertThat(newOrder().getSource()).isEqualTo(OrderSource.POS);
        assertThat(newOrder().getExternalOrderId()).isNull();
    }

    @Test
    void entersZomatoOrderWithItsIdAndKeepsSourceWhenLeftOut() {
        Order order = Order.open("1", null, online(OrderSource.ZOMATO, " 5512345 "), "user-1");
        assertThat(order.getSource()).isEqualTo(OrderSource.ZOMATO);
        assertThat(order.getExternalOrderId()).isEqualTo("5512345");

        order.updateDetails(new CustomerDetails(null, "Ravi", null, null, null, null, "5512345"));
        assertThat(order.getSource()).isEqualTo(OrderSource.ZOMATO);
        assertThat(order.getOrderType()).isEqualTo(OrderType.DELIVERY);
    }

    @Test
    void onlineOrderIdNeedsAnOnlineSource() {
        assertThatThrownBy(() -> Order.open("1", null, online(OrderSource.POS, "5512345"), "user-1"))
                .hasMessageContaining("only for Zomato / Swiggy");
    }

    @Test
    void aggregatorPaymentOnlyOnOnlineOrders() {
        Order counter = newOrder();
        counter.addLine(line(1, "100", "1"), NO_ROUND);
        assertThatThrownBy(() -> counter.recordPayment(pay(PaymentMethod.AGGREGATOR, "100", null), "user-1"))
                .hasMessageContaining("only for Zomato / Swiggy");

        Order swiggy = Order.open("2", null, online(OrderSource.SWIGGY, "SW-1"), "user-1");
        swiggy.addLine(line(1, "100", "1"), NO_ROUND);
        swiggy.recordPayment(pay(PaymentMethod.AGGREGATOR, "100", null), "user-1");
        assertThat(swiggy.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        assertThatThrownBy(() -> swiggy.updateDetails(new CustomerDetails(null, null, null, null, null,
                OrderSource.POS, null))).hasMessageContaining("must stay an online order");
    }
}
