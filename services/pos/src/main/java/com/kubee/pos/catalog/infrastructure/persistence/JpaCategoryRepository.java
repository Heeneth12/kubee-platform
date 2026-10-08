package com.kubee.pos.catalog.infrastructure.persistence;

import com.kubee.pos.catalog.domain.Category;
import com.kubee.pos.catalog.domain.CategoryRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data implements the domain's {@link CategoryRepository}. Tenant filtering is done by Hibernate. */
interface JpaCategoryRepository extends JpaRepository<Category, Long>, CategoryRepository {
}
