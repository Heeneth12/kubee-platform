package com.kubee.pos.reporting.application.query;

import com.kubee.pos.common.cqrs.Query;

public record ChannelSalesQuery(ReportPeriod period) implements Query<ChannelSalesReport> {
}
