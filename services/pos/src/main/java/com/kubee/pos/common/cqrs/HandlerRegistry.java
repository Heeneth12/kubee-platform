package com.kubee.pos.common.cqrs;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.ResolvableType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Maps a message class (command or query) to the single handler bean declared for it. */
final class HandlerRegistry<H> {

    private final Map<Class<?>, H> handlers = new HashMap<>();
    private final String kind;

    HandlerRegistry(Class<?> handlerType, List<H> beans, String kind) {
        this.kind = kind;
        for (H bean : beans) {
            Class<?> messageType = ResolvableType.forClass(AopUtils.getTargetClass(bean))
                    .as(handlerType)
                    .getGeneric(0)
                    .resolve();
            if (messageType == null) {
                throw new IllegalStateException("Cannot resolve " + kind + " type of " + bean.getClass().getName());
            }
            H previous = handlers.putIfAbsent(messageType, bean);
            if (previous != null) {
                throw new IllegalStateException("Two " + kind + " handlers for " + messageType.getName() + ": "
                        + AopUtils.getTargetClass(previous).getName() + ", " + AopUtils.getTargetClass(bean).getName());
            }
        }
    }

    H get(Class<?> messageType) {
        H handler = handlers.get(messageType);
        if (handler == null) {
            throw new IllegalStateException("No " + kind + " handler registered for " + messageType.getName());
        }
        return handler;
    }
}
