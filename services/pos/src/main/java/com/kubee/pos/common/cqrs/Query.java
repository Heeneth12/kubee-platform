package com.kubee.pos.common.cqrs;

/** A request to read state. Never changes anything. {@code R} is the view returned. */
public interface Query<R> {
}
