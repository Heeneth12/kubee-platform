package com.kubee.pos.shift.domain;

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

/** Cash put into or taken out of the drawer during a shift. Only created through its {@link Shift}. */
@Getter
@Entity
@Table(name = "cash_movements")
@SQLRestriction("is_deleted = false")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CashMovement extends BaseEntity {

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 10)
    private CashMovementType movementType;

    @Column(name = "amount", nullable = false)
    private Money amount;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "moved_at", nullable = false)
    private LocalDateTime movedAt;

    CashMovement(Shift shift, CashMovementType type, Money amount, String reason, String userUuid) {
        Guard.isTrue(type != null, "Cash in or cash out?");
        Guard.isTrue(amount != null && amount.amount().signum() > 0, "Amount must be more than 0");
        this.shift = shift;
        this.movementType = type;
        this.amount = amount;
        this.reason = Guard.requireText(reason, "Reason", 255);
        this.createdBy = userUuid;
        this.movedAt = LocalDateTime.now();
    }
}
