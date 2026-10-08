package com.kubee.pos.common.time;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.TimeZone;

/**
 * The shop's clock. Every timestamp in the POS is stored as shop-local time without a zone
 * ({@code TIMESTAMP} columns), so the whole app must run in the shop's zone: {@link #useShopZone()} sets the
 * JVM default at startup, which Java's {@code LocalDateTime.now()}, Hibernate and the PostgreSQL session
 * (the JDBC driver sends the JVM zone) all follow. Day boundaries for order tokens, bill numbers and
 * reports then fall at the shop's midnight even on a UTC server.
 */
public final class ShopTime {

    /** Override with the POS_TIMEZONE environment variable. */
    public static final String DEFAULT_ZONE = "Asia/Kolkata";

    private ShopTime() {
    }

    /** Call first thing in {@code main}, before anything reads the clock or opens a DB connection. */
    public static void useShopZone() {
        String zone = System.getenv().getOrDefault("POS_TIMEZONE", DEFAULT_ZONE);
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(zone)));
    }

    public static ZoneId zone() {
        return ZoneId.systemDefault();
    }

    public static LocalDate today() {
        return LocalDate.now(zone());
    }
}
