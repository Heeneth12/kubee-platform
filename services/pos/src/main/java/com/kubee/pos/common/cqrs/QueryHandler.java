package com.kubee.pos.common.cqrs;

/** Handles exactly one query type. Implementations are Spring beans, picked up by {@link QueryBus}. */
public interface QueryHandler<Q extends Query<R>, R> {

    R handle(Q query);
}
