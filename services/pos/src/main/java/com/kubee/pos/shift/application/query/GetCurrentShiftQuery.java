package com.kubee.pos.shift.application.query;

import com.kubee.pos.common.cqrs.Query;

/** The open shift, if any. */
public record GetCurrentShiftQuery() implements Query<ShiftView> {
}
