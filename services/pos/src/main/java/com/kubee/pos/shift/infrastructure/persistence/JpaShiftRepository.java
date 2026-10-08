package com.kubee.pos.shift.infrastructure.persistence;

import com.kubee.pos.shift.domain.Shift;
import com.kubee.pos.shift.domain.ShiftRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data implements the domain's {@link ShiftRepository}. Tenant filtering is done by Hibernate. */
interface JpaShiftRepository extends JpaRepository<Shift, Long>, ShiftRepository {
}
