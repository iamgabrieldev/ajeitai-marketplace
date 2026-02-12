package com.ajeitai.backend.infra.security;

/**
 * Armazena o {@link UserContext} associado à requisição atual.
 *
 * Usa ThreadLocal porque o stack padrão do Spring MVC + JPA
 * é baseado em thread por requisição. Em ambientes reativos,
 * seria necessário um mecanismo diferente (ex.: Reactor Context).
 */
public final class UserContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    private UserContextHolder() {
        // utility
    }

    public static void set(UserContext context) {
        CONTEXT.set(context);
    }

    public static UserContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}

