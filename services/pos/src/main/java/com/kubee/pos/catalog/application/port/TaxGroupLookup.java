package com.kubee.pos.catalog.application.port;

import java.util.Optional;

/** What catalog needs from the tax module: turn a tax group uuid into its id. */
public interface TaxGroupLookup {

    Optional<Long> findIdByUuid(String taxGroupUuid);
}
