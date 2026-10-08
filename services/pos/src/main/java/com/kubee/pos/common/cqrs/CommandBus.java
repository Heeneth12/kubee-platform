package com.kubee.pos.common.cqrs;

public interface CommandBus {

    <R> R send(Command<R> command);
}
