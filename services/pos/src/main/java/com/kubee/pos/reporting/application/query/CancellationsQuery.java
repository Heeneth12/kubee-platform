package com.kubee.pos.reporting.application.query;

import com.kubee.pos.common.cqrs.Query;

public record CancellationsQuery(ReportPeriod period) implements Query<CancellationReport> {
}
