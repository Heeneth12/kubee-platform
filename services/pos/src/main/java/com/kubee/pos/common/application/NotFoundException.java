package com.kubee.pos.common.application;

/** The requested record does not exist for this tenant. Mapped to HTTP 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String what, String uuid) {
        return new NotFoundException(what + " not found: " + uuid);
    }
}
