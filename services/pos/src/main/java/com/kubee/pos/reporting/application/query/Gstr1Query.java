package com.kubee.pos.reporting.application.query;

import com.kubee.pos.common.cqrs.Query;

import java.time.YearMonth;
import java.util.Map;

/** GSTR-1 JSON (GSTN offline tool layout) for one month. */
public record Gstr1Query(YearMonth month) implements Query<Map<String, Object>> {
}
