package com.kubee.pos.shift.domain;

import com.kubee.pos.common.domain.AggregateRoot;
import com.kubee.pos.common.domain.Guard;
import com.kubee.pos.common.domain.Money;
import com.kubee.pos.shift.domain.event.ShiftClosed;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One cash-drawer session: open with the cash in the drawer, record cash put in / taken out, close by
 * counting the cash. Expected cash = opening + net cash sales + cash in − cash out; the difference
 * (counted − expected) shows excess or shortage. A closed shift never changes.
 */
@Getter
@Entity
@Table(name = "shifts")
@SQLRestriction("is_deleted = false")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shift extends AggregateRoot {

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private ShiftStatus status;

    @Column(name = "opened_by", length = 36)
    private String openedBy;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "opening_cash", nullable = false)
    private Money openingCash;

    @Column(name = "opening_notes", columnDefinition = "text")
    private String openingNotes;

    @Column(name = "closed_by", length = 36)
    private String closedBy;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    /** Cash payments − cash refunds during the shift; stored at close. */
    @Column(name = "cash_sales")
    private Money cashSales;

    @Column(name = "cash_in")
    private Money cashIn;

    @Column(name = "cash_out")
    private Money cashOut;

    @Column(name = "expected_cash")
    private Money expectedCash;

    @Column(name = "counted_cash")
    private Money countedCash;

    /** Counted − expected: negative means cash is short. */
    @Column(name = "cash_difference")
    private Money cashDifference;

    @Column(name = "closing_notes", columnDefinition = "text")
    private String closingNotes;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "shift", cascade = CascadeType.ALL)
    @OrderBy("movedAt ASC, id ASC")
    private List<CashMovement> movements = new ArrayList<>();

    public static Shift open(Money openingCash, String notes, String userUuid) {
        Guard.isTrue(openingCash != null && !openingCash.isNegative(), "Opening cash cannot be negative");
        Shift shift = new Shift();
        shift.status = ShiftStatus.OPEN;
        shift.openedBy = userUuid;
        shift.openedAt = LocalDateTime.now();
        shift.openingCash = openingCash;
        shift.openingNotes = Guard.trimToNull(notes);
        return shift;
    }

    public CashMovement recordCash(CashMovementType type, Money amount, String reason, String userUuid) {
        requireOpen();
        CashMovement movement = new CashMovement(this, type, amount, reason, userUuid);
        movements.add(movement);
        return movement;
    }

    /**
     * @param cashSales net cash taken during the shift (cash payments − cash refunds between open and now),
     *                  worked out from the payments by the caller
     */
    public void close(Money countedCash, Money cashSales, String notes, String userUuid) {
        requireOpen();
        Guard.isTrue(countedCash != null && !countedCash.isNegative(), "Counted cash cannot be negative");
        Guard.isTrue(cashSales != null, "Cash sales are required");
        this.cashSales = cashSales;
        this.cashIn = total(CashMovementType.IN);
        this.cashOut = total(CashMovementType.OUT);
        this.expectedCash = expectedCash(cashSales);
        this.countedCash = countedCash;
        this.cashDifference = countedCash.minus(expectedCash);
        this.closingNotes = Guard.trimToNull(notes);
        this.closedBy = userUuid;
        this.closedAt = LocalDateTime.now();
        this.status = ShiftStatus.CLOSED;
        registerEvent(new ShiftClosed(getUuid(), expectedCash, countedCash, cashDifference));
    }

    /** What should be in the drawer, given the net cash sales so far. */
    public Money expectedCash(Money cashSales) {
        return openingCash.plus(cashSales).plus(total(CashMovementType.IN)).minus(total(CashMovementType.OUT));
    }

    public Money total(CashMovementType type) {
        return movements.stream()
                .filter(m -> m.getMovementType() == type)
                .map(CashMovement::getAmount)
                .reduce(Money.ZERO, Money::plus);
    }

    public List<CashMovement> getMovements() {
        return List.copyOf(movements);
    }

    private void requireOpen() {
        Guard.isTrue(status == ShiftStatus.OPEN, "This shift is already closed");
    }
}
