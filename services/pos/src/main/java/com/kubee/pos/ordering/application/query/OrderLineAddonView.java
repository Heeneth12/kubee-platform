package com.kubee.pos.ordering.application.query;

import java.math.BigDecimal;

public record OrderLineAddonView(String uuid, String addonUuid, String name, BigDecimal quantity, BigDecimal unitPrice) {
}
