package com.kubee.pos.common.application;

/** The change clashes with existing data (duplicate code, record still in use). Mapped to HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
