package com.kubee.pos.shift.domain;

import java.util.Optional;

public interface ShiftRepository {

    Shift save(Shift shift);

    Optional<Shift> findByUuid(String uuid);

    Optional<Shift> findFirstByStatus(ShiftStatus status);
}
