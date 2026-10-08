package com.kubee.pos.common.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {

    @Test
    void normalisesToTwoDecimalsHalfUp() {
        assertThat(Money.of("10.005").amount()).isEqualByComparingTo("10.01");
        assertThat(Money.of("10").amount().scale()).isEqualTo(2);
    }

    @Test
    void equalAmountsWithDifferentScaleAreEqual() {
        assertThat(Money.of("60")).isEqualTo(Money.of(new BigDecimal("60.000")));
    }

    @Test
    void arithmetic() {
        assertThat(Money.of("60").plus(Money.of("20.50"))).isEqualTo(Money.of("80.50"));
        assertThat(Money.of("60").minus(Money.of("70")).isNegative()).isTrue();
        assertThat(Money.of("60").times(new BigDecimal("1.5"))).isEqualTo(Money.of("90"));
        assertThat(Money.of("100").isGreaterThan(Money.of("99.99"))).isTrue();
    }

    @Test
    void ofNullableKeepsNull() {
        assertThat(Money.ofNullable(null)).isNull();
    }
}
