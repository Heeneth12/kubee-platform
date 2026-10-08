package com.kubee.pos.reporting.application.query;

import com.kubee.pos.common.cqrs.Query;

public record PaymentModesQuery(ReportPeriod period) implements Query<PaymentModeReport> {
}
