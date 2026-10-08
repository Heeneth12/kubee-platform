package com.kubee.pos.common.domain;

import java.util.regex.Pattern;

/** Small argument checks used by aggregates to protect their own invariants. */
public final class Guard {

    private Guard() {
    }

    public static String requireText(String value, String field, int maxLength) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new DomainException(field + " is required");
        }
        return maxLength(trimmed, field, maxLength);
    }

    public static String optionalText(String value, String field, int maxLength) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : maxLength(trimmed, field, maxLength);
    }

    public static String optionalPattern(String value, String field, Pattern pattern, String hint) {
        String trimmed = trimToNull(value);
        if (trimmed != null && !pattern.matcher(trimmed).matches()) {
            throw new DomainException(field + " " + hint);
        }
        return trimmed;
    }

    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new DomainException(message);
        }
    }

    public static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String maxLength(String value, String field, int maxLength) {
        if (value.length() > maxLength) {
            throw new DomainException(field + " must be at most " + maxLength + " characters");
        }
        return value;
    }
}
