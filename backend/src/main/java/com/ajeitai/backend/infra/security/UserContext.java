package com.ajeitai.backend.infra.security;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Informações essenciais sobre o usuário autenticado,
 * extraídas do JWT e disponíveis durante o ciclo de vida da requisição.
 *
 * Este contexto é deliberadamente enxuto: detalhes de domínio
 * (como entidades Cliente/Prestador) continuam sendo resolvidos
 * pelos serviços apropriados.
 */
public class UserContext {

    private final String keycloakId;
    private final UserType userType;
    private final Set<String> roles;
    // IDs internos resolvidos a partir do keycloakId (opcionais, populados sob demanda)
    private Long clienteId;
    private Long prestadorId;

    public UserContext(String keycloakId, UserType userType, Set<String> roles) {
        this.keycloakId = keycloakId;
        this.userType = userType != null ? userType : UserType.UNKNOWN;
        this.roles = roles != null ? new HashSet<>(roles) : Collections.emptySet();
    }

    public String getKeycloakId() {
        return keycloakId;
    }

    public UserType getUserType() {
        return userType;
    }

    public Set<String> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public boolean hasRole(String role) {
        if (role == null) return false;
        return roles.contains(role) || roles.contains("ROLE_" + role);
    }

    public boolean isCliente() {
        return userType == UserType.CLIENTE;
    }

    public boolean isPrestador() {
        return userType == UserType.PRESTADOR;
    }

    public boolean isAdmin() {
        return userType == UserType.ADMIN;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public Long getPrestadorId() {
        return prestadorId;
    }

    public void setPrestadorId(Long prestadorId) {
        this.prestadorId = prestadorId;
    }
}

