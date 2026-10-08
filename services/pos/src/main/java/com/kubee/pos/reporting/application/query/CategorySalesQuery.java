package com.kubee.pos.reporting.application.query;

import com.kubee.pos.common.cqrs.Query;

public record CategorySalesQuery(ReportPeriod period) implements Query<CategorySalesReport> {
}
