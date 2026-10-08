package com.kubee.pos.ordering.application.port;

import com.kubee.pos.ordering.domain.RoundOffMode;

/** Shop settings that affect orders. */
public interface OrderSettingsPort {

    /** The shop's round-off rule; NEAREST_1 when the shop has not set one. */
    RoundOffMode roundOffMode();
}
