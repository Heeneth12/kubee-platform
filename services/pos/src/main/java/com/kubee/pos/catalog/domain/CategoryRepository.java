package com.kubee.pos.catalog.domain;

import java.util.Optional;

public interface CategoryRepository {

    Category save(Category category);

    Optional<Category> findByUuid(String uuid);
}
