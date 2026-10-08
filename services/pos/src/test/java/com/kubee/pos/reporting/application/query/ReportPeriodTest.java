package com.kubee.pos.reporting.application.query;

import com.kubee.pos.common.domain.DomainException;
import com.kubee.pos.common.time.ShopTime;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportPeriodTest {

    private static final LocalDate TODAY = ShopTime.today();

    @Test
    void defaultsToToday() {
        var period = new ReportPeriod(null, null);
        assertThat(period.from()).isEqualTo(TODAY);
        assertThat(period.to()).isEqualTo(TODAY);
        assertThat(period.end()).isEqualTo(TODAY.plusDays(1).atStartOfDay());
    }

    @Test
    void openEndedRangesRunToToday() {
        var period = new ReportPeriod(TODAY.minusDays(6), null);
        assertThat(period.to()).isEqualTo(TODAY);
        assertThat(new ReportPeriod(null, LocalDate.of(2026, 4, 1)).from()).isEqualTo(LocalDate.of(2026, 4, 1));
    }

    @Test
    void rejectsBackwardsAndTooLongRanges() {
        assertThatThrownBy(() -> new ReportPeriod(LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 1)))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new ReportPeriod(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 6, 1)))
                .hasMessageContaining("366");
        new ReportPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31)); // a full financial year is fine
    }
}
