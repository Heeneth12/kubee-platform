package com.kubee.pos.common.cqrs;

/** Handles exactly one command type. Implementations are Spring beans, picked up by {@link CommandBus}. */
public interface CommandHandler<C extends Command<R>, R> {

    R handle(C command);
}
