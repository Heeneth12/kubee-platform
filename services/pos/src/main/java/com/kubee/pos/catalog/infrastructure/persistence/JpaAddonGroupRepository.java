package com.kubee.pos.catalog.infrastructure.persistence;

import com.kubee.pos.catalog.domain.AddonGroup;
import com.kubee.pos.catalog.domain.AddonGroupRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data implements the domain's {@link AddonGroupRepository}. Tenant filtering is done by Hibernate. */
interface JpaAddonGroupRepository extends JpaRepository<AddonGroup, Long>, AddonGroupRepository {
}
