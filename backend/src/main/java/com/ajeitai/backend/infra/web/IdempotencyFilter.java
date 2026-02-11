package com.ajeitai.backend.infra.web;

import com.ajeitai.backend.infra.exception.ErroResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Idempotência via header Idempotency-Key: evita processar duas vezes a mesma operação.
 * Para POST/PUT, se a chave já foi processada, retorna 409. Se em processamento, 409 com Retry-After.
 */
@Component
@Order(3)
@ConditionalOnBean(RedisTemplate.class)
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String PREFIX = "idem:";
    private static final String SUFFIX_PROCESSING = ":proc";
    private static final int PROCESSING_TTL_SECONDS = 120;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final long ttlSeconds;

    public IdempotencyFilter(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            @org.springframework.beans.factory.annotation.Value("${app.idempotency.ttl-seconds:86400}") long ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String key = request.getHeader("Idempotency-Key");
        if (key == null || key.isBlank()) {
            return true;
        }
        String method = request.getMethod();
        return !("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String rawKey = request.getHeader("Idempotency-Key").trim();
        String sanitized = rawKey.replaceAll("[^a-zA-Z0-9_-]", "_");
        if (sanitized.isEmpty()) {
            sanitized = String.valueOf(rawKey.hashCode());
        }
        String idemKey = PREFIX + sanitized.substring(0, Math.min(128, sanitized.length()));
        String processingKey = idemKey + SUFFIX_PROCESSING;

        Boolean alreadyProcessing = redisTemplate.opsForValue().setIfAbsent(processingKey, "1", PROCESSING_TTL_SECONDS, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(alreadyProcessing)) {
            response.setStatus(HttpStatus.CONFLICT.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(PROCESSING_TTL_SECONDS));
            ErroResponse body = new ErroResponse(
                    "IDEMPOTENCIA_EM_PROCESSAMENTO",
                    "Requisição duplicada em processamento. Aguarde e tente novamente.",
                    request.getRequestURI(),
                    Instant.now(),
                    org.slf4j.MDC.get("requestId")
            );
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }

        String cached = redisTemplate.opsForValue().get(idemKey);
        if (cached != null) {
            try {
                CachedResponse cr = objectMapper.readValue(cached, CachedResponse.class);
                response.setStatus(cr.status());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                if (cr.body() != null && !cr.body().isEmpty()) {
                    response.getWriter().write(cr.body());
                }
            } catch (Exception e) {
                response.setStatus(HttpStatus.CONFLICT.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                ErroResponse body = new ErroResponse(
                        "IDEMPOTENCIA_DUPLICADA",
                        "Requisição duplicada. Use o resultado da requisição anterior.",
                        request.getRequestURI(),
                        Instant.now(),
                        org.slf4j.MDC.get("requestId")
                );
                response.getWriter().write(objectMapper.writeValueAsString(body));
            }
            redisTemplate.delete(processingKey);
            return;
        }

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, responseWrapper);
        } finally {
            int status = responseWrapper.getStatus();
            byte[] content = responseWrapper.getContentAsByteArray();
            if (status >= 200 && status < 300 && content != null && content.length > 0) {
                String contentType = responseWrapper.getContentType();
                if (contentType != null && contentType.contains("json")) {
                    String body = new String(content, StandardCharsets.UTF_8);
                    try {
                        CachedResponse cr = new CachedResponse(status, body);
                        redisTemplate.opsForValue().set(idemKey, objectMapper.writeValueAsString(cr), ttlSeconds, TimeUnit.SECONDS);
                    } catch (Exception ignored) {
                    }
                }
            }
            redisTemplate.delete(processingKey);
            responseWrapper.copyBodyToResponse();
        }
    }

    private record CachedResponse(int status, String body) {}
}
