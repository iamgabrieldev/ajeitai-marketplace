package com.ajeitai.backend.infra.security;

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
@EnableMethodSecurity
public class SecurityConfig {

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
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/index.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**"))
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
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> {
                    // ROTAS PÚBLICAS (INCLUINDO VINCULAR PARA MVP)
                    authorize.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll();
                    authorize.requestMatchers("/publico/**", "/api/auth/**", "/api/categorias-atuacao", "/api/webhooks/**").permitAll();
                    authorize.requestMatchers("/clientes/vincular").permitAll(); // BYPASS APLICADO CORRETAMENTE AQUI

                    if (isDev) {
                        authorize.requestMatchers("/swagger-ui.html", "/swagger-ui/index.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**", "/h2-console/**").permitAll();
                    }
                    authorize.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtConverter()))
                )
                .addFilterAfter(new UserContextFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new PrestadorAssinaturaFilter(assinaturaService), UserContextFilter.class);

        if (isDev) {
            http.headers(headers -> headers.frameOptions(frame -> frame.disable()));
        }

        return http.build();
    }

    private Converter<Jwt, AbstractAuthenticationToken> keycloakJwtConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<GrantedAuthority> authorities = new java.util.ArrayList<>();
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
        config.addAllowedOrigin("https://app.iamgabrieldev.com.br");
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // AQUI ESTAVA O ERRO DO CORS: AGORA ELE ACEITA QUALQUER ROTA, NÃO APENAS /api/**
        source.registerCorsConfiguration("/**", config); 
        return source;
    }
}
