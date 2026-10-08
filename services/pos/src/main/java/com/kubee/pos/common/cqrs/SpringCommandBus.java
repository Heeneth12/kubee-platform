package com.kubee.pos.common.cqrs;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
class SpringCommandBus implements CommandBus {

    private final HandlerRegistry<CommandHandler<?, ?>> registry;

    SpringCommandBus(List<CommandHandler<?, ?>> handlers) {
        this.registry = new HandlerRegistry<>(CommandHandler.class, handlers, "command");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R send(Command<R> command) {
        var handler = (CommandHandler<Command<R>, R>) registry.get(command.getClass());
        return handler.handle(command);
    }
}
