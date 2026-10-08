package com.kubee.pos.billing.application.port;

import com.kubee.pos.billing.domain.TaxComponentSpec;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** The parts of each tax group (CGST 2.5 + SGST 2.5 ...). */
public interface TaxComponentPort {

    Map<Long, List<TaxComponentSpec>> componentsOf(Collection<Long> taxGroupIds);
}
