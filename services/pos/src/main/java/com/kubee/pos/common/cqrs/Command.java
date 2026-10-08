package com.kubee.pos.common.cqrs;

/** A request to change state. {@code R} is what the handler returns (often the aggregate uuid). */
public interface Command<R> {
}
