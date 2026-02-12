package com.ajeitai.backend.infra.security;

import com.ajeitai.backend.infra.security.PrestadorAssinaturaFilter;
import com.ajeitai.backend.service.AssinaturaService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Permite usar @PreAuthorize nos controllers depois
public class SecurityConfig {

    /**
     * Chain com prioridade maior: Swagger/docs sem OAuth2.
     * OAuth2 Resource Server valida JWT antes do permitAll; excluir docs dessa chain resolve.
     * H2 permanece na chain principal pois já funciona corretamente.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain swaggerFilterChain(HttpSecurity http, Environment environment) throws Exception {
        if (!environment.acceptsProfiles(Profiles.of("dev"))) {
            return http.securityMatchers(matchers -> matchers.requestMatchers("/impossible-path-never-matches/**"))
                    .authorizeHttpRequests(a -> a.anyRequest().denyAll())
                    .build();
        }
        return http
                .securityMatchers(matchers -> matchers
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/index.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        ))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http, Environment environment, AssinaturaService assinaturaService) throws Exception {
        boolean isDev = environment.acceptsProfiles(Profiles.of("dev"));

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable()) // <--- 1. DESATIVA CSRF (Crucial para API)
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll();
                    authorize.requestMatchers("/publico/**", "/api/auth/**", "/api/categorias-atuacao", "/api/webhooks/**").permitAll();
                    if (isDev) {
                        authorize.requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/index.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/h2-console/**"
                        ).permitAll();
                    }
                    authorize.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtConverter())) // <--- 2. CONVERSOR DE ROLES
                )
                .addFilterAfter(new UserContextFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new PrestadorAssinaturaFilter(assinaturaService), UserContextFilter.class);

        if (isDev) {
            http.headers(headers -> headers.frameOptions(frame -> frame.disable()));
        }

        return http.build();
    }

    // Ensina o Spring a pegar as roles dentro de "realm_access" -> "roles"
    private Converter<Jwt, AbstractAuthenticationToken> keycloakJwtConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<GrantedAuthority> authorities = new java.util.ArrayList<>();

            // Mapeia scopes padroes (ex: SCOPE_email)
            // Se quiser ignorar scopes e usar só roles, pode remover essa parte ou ajustar

            // Extrai as roles do realm_access
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null) {
                Object rolesClaim = realmAccess.get("roles");
                if (rolesClaim instanceof Collection<?> roles) {
                    authorities.addAll(roles.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .collect(Collectors.toList()));
                }
            }
            return authorities;
        });
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedOrigin("http://127.0.0.1:3000");
        config.addAllowedHeader("*");
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("PATCH");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
