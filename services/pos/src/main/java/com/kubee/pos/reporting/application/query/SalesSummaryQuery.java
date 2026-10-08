package com.kubee.pos.reporting.application.query;

import com.kubee.pos.common.cqrs.Query;

public record SalesSummaryQuery(ReportPeriod period) implements Query<SalesSummaryReport> {
}
