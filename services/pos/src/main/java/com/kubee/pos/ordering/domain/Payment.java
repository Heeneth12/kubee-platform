package com.kubee.pos.ordering.domain;

import com.kubee.pos.common.domain.BaseEntity;
import com.kubee.pos.common.domain.Guard;
import com.kubee.pos.common.domain.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * Money in (PAYMENT) or money back (REFUND) against an order. Several payments = split payment.
 * Rows are never edited after they are recorded; a mistake is fixed with a refund.
 * Only created through its {@link Order}.
 */
@Getter
@Entity
@Table(name = "payments")
@SQLRestriction("is_deleted = false")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "client_ref", length = 64)
    private String clientRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 10)
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "amount", nullable = false)
    private Money amount;

    @Column(name = "tendered_amount")
    private Money tenderedAmount;

    @Column(name = "change_amount")
    private Money changeAmount;

    @Column(name = "reference_no", length = 100)
    private String referenceNo;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_of_payment_id")
    private Payment refundOf;

    @Column(name = "notes")
    private String notes;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    @Column(name = "received_by", length = 36)
    private String receivedBy;

    private Payment(Order order, PaymentType type, PaymentMethod method, Money amount, String clientRef,
                    String referenceNo, String notes, String userUuid) {
        Guard.isTrue(method != null, "Payment method is required");
        Guard.isTrue(amount != null && amount.amount().signum() > 0, "Amount must be more than 0");
        this.order = order;
        this.paymentType = type;
        this.paymentMethod = method;
        this.status = TransactionStatus.SUCCESS;
        this.amount = amount;
        this.clientRef = Guard.optionalText(clientRef, "Client reference", 64);
        this.referenceNo = Guard.optionalText(referenceNo, "Reference number", 100);
        this.notes = Guard.optionalText(notes, "Payment note", 255);
        this.paidAt = LocalDateTime.now();
        this.receivedBy = userUuid;
    }

    static Payment payment(Order order, PaymentSpec spec, Money amount, Money tendered, String userUuid) {
        Payment payment = new Payment(order, PaymentType.PAYMENT, spec.method(), amount, spec.clientRef(),
                spec.referenceNo(), spec.notes(), userUuid);
        if (tendered != null) {
            payment.tenderedAmount = tendered;
            payment.changeAmount = tendered.minus(amount);
        }
        return payment;
    }

    static Payment refund(Order order, Payment original, Money amount, PaymentMethod method, String reason,
                          String clientRef, String userUuid) {
        Payment refund = new Payment(order, PaymentType.REFUND, method == null ? original.paymentMethod : method,
                amount, clientRef, null, Guard.requireText(reason, "Refund reason", 255), userUuid);
        refund.refundOf = original;
        return refund;
    }

    public boolean isSuccessful() {
        return status == TransactionStatus.SUCCESS && !isDeleted();
    }

    /** Compared by uuid: {@code refundOf} may be a Hibernate proxy of the same row. */
    boolean isRefundOf(Payment payment) {
        return paymentType == PaymentType.REFUND && refundOf != null && refundOf.getUuid().equals(payment.getUuid());
    }
}
