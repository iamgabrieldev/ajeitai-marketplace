package com.ajeitai.backend.infra.web;

import com.ajeitai.backend.infra.exception.ErroResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Rate limit por IP (ou por identificador do cliente) para mitigar DDoS.
 * Só ativo quando app.rate-limit.enabled=true e Redis disponível.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true")
@ConditionalOnBean(RedisTemplate.class)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String PREFIX = "rl:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final int maxRequests;
    private final int windowSeconds;

    public RateLimitFilter(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.rate-limit.max-requests:200}") int maxRequests,
            @Value("${app.rate-limit.window-seconds:60}") int windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientId = clientId(request);
        String key = PREFIX + clientId;

        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
            }
            if (count != null && count > maxRequests) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                ErroResponse body = new ErroResponse(
                        "RATE_LIMIT",
                        "Muitas requisições. Tente novamente em alguns instantes.",
                        request.getRequestURI(),
                        Instant.now(),
                        MDC.get("requestId")
                );
                response.getWriter().write(objectMapper.writeValueAsString(body));
                return;
            }
        } catch (Exception e) {
            // Se Redis falhar, deixa a requisição passar para não derrubar o serviço
        }
        filterChain.doFilter(request, response);
    }

    private static String clientId(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim().replaceAll("[^a-zA-Z0-9.:]", "_");
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr().replace(":", "_") : "unknown";
    }
}
