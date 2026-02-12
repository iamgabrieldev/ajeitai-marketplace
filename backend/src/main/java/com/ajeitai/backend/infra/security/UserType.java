package com.ajeitai.backend.infra.security;

/**
 * Tipo de usuário autenticado no sistema, derivado das roles do Keycloak.
 */
public enum UserType {
    CLIENTE,
    PRESTADOR,
    ADMIN,
    UNKNOWN
}

