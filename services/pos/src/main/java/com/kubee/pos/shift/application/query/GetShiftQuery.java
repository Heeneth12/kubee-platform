package com.kubee.pos.shift.application.query;

import com.kubee.pos.common.cqrs.Query;

public record GetShiftQuery(String shiftUuid) implements Query<ShiftView> {
}
