package com.kubee.pos.reporting.application.query;

import com.kubee.pos.common.cqrs.Query;

public record ShiftsQuery(ReportPeriod period) implements Query<ShiftReport> {
}
