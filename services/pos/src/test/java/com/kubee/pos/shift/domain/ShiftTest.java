package com.kubee.pos.shift.domain;

import com.kubee.pos.common.domain.DomainException;
import com.kubee.pos.common.domain.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShiftTest {

    @Test
    void expectedCashIsOpeningPlusCashSalesPlusInMinusOut() {
        Shift shift = Shift.open(Money.of("2000"), "morning", "u1");
        shift.recordCash(CashMovementType.IN, Money.of("500"), "change from bank", "u1");
        shift.recordCash(CashMovementType.OUT, Money.of("300"), "milk supplier", "u1");

        assertThat(shift.expectedCash(Money.of("4200"))).isEqualTo(Money.of("6400"));

        shift.close(Money.of("6350"), Money.of("4200"), "50 short", "u2");
        assertThat(shift.getStatus()).isEqualTo(ShiftStatus.CLOSED);
        assertThat(shift.getCashIn()).isEqualTo(Money.of("500"));
        assertThat(shift.getCashOut()).isEqualTo(Money.of("300"));
        assertThat(shift.getExpectedCash()).isEqualTo(Money.of("6400"));
        assertThat(shift.getCashDifference()).isEqualTo(Money.of("-50"));
        assertThat(shift.getClosedBy()).isEqualTo("u2");
    }

    @Test
    void closedShiftCannotChange() {
        Shift shift = Shift.open(Money.ZERO, null, "u1");
        shift.close(Money.ZERO, Money.ZERO, null, "u1");

        assertThatThrownBy(() -> shift.recordCash(CashMovementType.IN, Money.of("1"), "x", "u1"))
                .hasMessageContaining("closed");
        assertThatThrownBy(() -> shift.close(Money.ZERO, Money.ZERO, null, "u1")).hasMessageContaining("closed");
    }

    @Test
    void validatesAmounts() {
        assertThatThrownBy(() -> Shift.open(Money.of("-1"), null, "u")).isInstanceOf(DomainException.class);
        Shift shift = Shift.open(Money.of("100"), null, "u");
        assertThatThrownBy(() -> shift.recordCash(CashMovementType.OUT, Money.ZERO, "x", "u"))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> shift.recordCash(CashMovementType.OUT, Money.of("10"), " ", "u"))
                .hasMessageContaining("Reason");
        assertThatThrownBy(() -> shift.close(Money.of("-5"), Money.ZERO, null, "u"))
                .isInstanceOf(DomainException.class);
    }
}
