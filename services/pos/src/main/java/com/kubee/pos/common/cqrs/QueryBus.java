package com.kubee.pos.common.cqrs;

public interface QueryBus {

    <R> R ask(Query<R> query);
}
