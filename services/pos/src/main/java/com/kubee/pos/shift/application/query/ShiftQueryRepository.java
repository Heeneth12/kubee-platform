package com.kubee.pos.shift.application.query;

import java.util.Optional;

public interface ShiftQueryRepository {

    Optional<ShiftView> findShift(String shiftUuid);

    Optional<ShiftView> findOpenShift();
}
