package com.ajeitai.backend.infra.security;

import com.ajeitai.backend.service.AssinaturaService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Para requisições de prestador (role prestador) em /api/prestadores/**,
 * exige assinatura ativa, exceto para: vincular, me/assinatura e GET me.
 */
public class PrestadorAssinaturaFilter extends OncePerRequestFilter {

    private final AssinaturaService assinaturaService;

    public PrestadorAssinaturaFilter(AssinaturaService assinaturaService) {
        this.assinaturaService = assinaturaService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/prestadores")) {
            filterChain.doFilter(request, response);
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_prestador"))) {
            filterChain.doFilter(request, response);
            return;
        }
        if (isAllowedWithoutSubscription(request, path)) {
            filterChain.doFilter(request, response);
            return;
        }
        String keycloakId = null;
        if (auth.getPrincipal() instanceof Jwt jwt) {
            keycloakId = jwt.getSubject();
        }
        if (keycloakId != null && !assinaturaService.prestadorComAssinaturaAtiva(keycloakId)) {
            response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
            response.setContentType("application/json;charset=UTF-8");
            String body = "{\"message\":\"Sua assinatura está inativa. Regularize para continuar atendendo.\"}";
            response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAllowedWithoutSubscription(HttpServletRequest request, String path) {
        if ("POST".equalsIgnoreCase(request.getMethod()) && path.endsWith("/vincular")) {
            return true;
        }
        if (path.contains("/me/assinatura")) {
            return true;
        }
        if ("GET".equalsIgnoreCase(request.getMethod()) && ("/api/prestadores/me".equals(path) || "/api/prestadores/me/".equals(path))) {
            return true;
        }
        return false;
    }
}
