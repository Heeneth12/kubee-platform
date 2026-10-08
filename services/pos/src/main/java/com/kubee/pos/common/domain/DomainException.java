package com.kubee.pos.common.domain;

/** A business rule was broken (e.g. selling price above MRP). Mapped to HTTP 422. */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
