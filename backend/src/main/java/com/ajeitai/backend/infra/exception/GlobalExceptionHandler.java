package com.ajeitai.backend.infra.exception;

import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String REQUEST_ID_MDC = "requestId";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidation(MethodArgumentNotValidException ex,
                                                         HttpServletRequest request) {
        List<ErroResponse.CampoErro> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErroResponse.CampoErro(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());
        String msg = "Erro de validação";
        ErroResponse body = new ErroResponse(
                "VALIDACAO",
                msg,
                request.getRequestURI(),
                Instant.now(),
                requestId(),
                erros
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> handleAccessDenied(AccessDeniedException ex,
                                                           HttpServletRequest request) {
        ErroResponse body = new ErroResponse(
                "ACESSO_NEGADO",
                "Acesso negado.",
                request.getRequestURI(),
                Instant.now(),
                requestId()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                             HttpServletRequest request) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Requisição inválida";
        boolean notFound = msg.contains("não encontrado");
        HttpStatus status = notFound ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        ErroResponse body = new ErroResponse(
                notFound ? "NAO_ENCONTRADO" : "REQUISICAO_INVALIDA",
                msg,
                request.getRequestURI(),
                Instant.now(),
                requestId()
        );
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        String requestId = requestId();
        log.error("Erro interno requestId={} path={}", requestId, request.getRequestURI(), ex);
        ErroResponse body = new ErroResponse(
                "ERRO_INTERNO",
                "Erro interno. Tente novamente mais tarde.",
                request.getRequestURI(),
                Instant.now(),
                requestId
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private static String requestId() {
        String id = MDC.get(REQUEST_ID_MDC);
        return id != null ? id : "";
    }
}
