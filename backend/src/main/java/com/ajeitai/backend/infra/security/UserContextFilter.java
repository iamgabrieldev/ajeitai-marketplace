package com.ajeitai.backend.infra.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Popula o {@link UserContextHolder} a partir do JWT autenticado
 * e das roles já convertidas em {@link GrantedAuthority}.
 */
public class UserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication instanceof JwtAuthenticationToken jwtAuth && authentication.isAuthenticated()) {
                Jwt jwt = jwtAuth.getToken();
                String keycloakId = jwt.getSubject();

                Set<String> roles = jwtAuth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet());

                UserType userType = resolveUserType(roles);

                UserContext context = new UserContext(keycloakId, userType, roles);
                UserContextHolder.set(context);
            }

            filterChain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }

    private UserType resolveUserType(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return UserType.UNKNOWN;
        }
        // Convenção: Keycloak realm roles -> ROLE_cliente, ROLE_prestador, ROLE_admin
        if (roles.contains("ROLE_admin") || roles.contains("ROLE_ADMIN")) {
            return UserType.ADMIN;
        }
        if (roles.contains("ROLE_prestador") || roles.contains("ROLE_PRESTADOR")) {
            return UserType.PRESTADOR;
        }
        if (roles.contains("ROLE_cliente") || roles.contains("ROLE_CLIENTE")) {
            return UserType.CLIENTE;
        }
        return UserType.UNKNOWN;
    }
}

