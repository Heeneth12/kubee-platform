package com.kubee.pos.reporting.application.query;

import com.kubee.pos.common.cqrs.Query;

public record GstSummaryQuery(ReportPeriod period) implements Query<GstReport> {
}
