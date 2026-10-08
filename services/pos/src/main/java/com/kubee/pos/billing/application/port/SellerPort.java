package com.kubee.pos.billing.application.port;

import java.util.Optional;

/** The shop's invoice settings (pos_settings). */
public interface SellerPort {

    Optional<SellerSettings> currentSeller();

    /** @param billPrefix printed before the series in the bill number, e.g. "KP" */
    record SellerSettings(String shopName, String address, String gstin, String stateCode, String billPrefix,
                          int financialYearStartMonth) {
    }
}
