package com.kubee.pos.common.cqrs;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
class SpringQueryBus implements QueryBus {

    private final HandlerRegistry<QueryHandler<?, ?>> registry;

    SpringQueryBus(List<QueryHandler<?, ?>> handlers) {
        this.registry = new HandlerRegistry<>(QueryHandler.class, handlers, "query");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R ask(Query<R> query) {
        var handler = (QueryHandler<Query<R>, R>) registry.get(query.getClass());
        return handler.handle(query);
    }
}
