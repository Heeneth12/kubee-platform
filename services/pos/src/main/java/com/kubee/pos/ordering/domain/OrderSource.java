package com.kubee.pos.ordering.domain;

/**
 * Where the order came from. {@code POS} = taken at this counter; the others are online orders the shop
 * accepted on the aggregator's partner app and entered here, so sales can be reported per channel.
 */
public enum OrderSource {
    POS, ZOMATO, SWIGGY, OTHER;

    public boolean isOnline() {
        return this != POS;
    }
}
