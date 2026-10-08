package com.kubee.pos.catalog.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AddonGroupRepository {

    AddonGroup save(AddonGroup addonGroup);

    Optional<AddonGroup> findByUuid(String uuid);

    List<AddonGroup> findByUuidIn(Collection<String> uuids);
}
