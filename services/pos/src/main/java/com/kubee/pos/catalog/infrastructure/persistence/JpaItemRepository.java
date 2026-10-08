package com.kubee.pos.catalog.infrastructure.persistence;

import com.kubee.pos.catalog.domain.Item;
import com.kubee.pos.catalog.domain.ItemRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data implements the domain's {@link ItemRepository}. Tenant filtering is done by Hibernate. */
interface JpaItemRepository extends JpaRepository<Item, Long>, ItemRepository {
}
