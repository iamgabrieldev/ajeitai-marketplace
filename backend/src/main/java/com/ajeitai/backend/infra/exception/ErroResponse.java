package com.ajeitai.backend.infra.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * DTO padronizado para respostas de erro da API.
 * Nunca inclui stackTrace no JSON de resposta.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(
        String codigo,
        String mensagem,
        String path,
        Instant timestamp,
        String requestId,
        List<CampoErro> erros
) {
    public ErroResponse(String codigo, String mensagem, String path, Instant timestamp, String requestId) {
        this(codigo, mensagem, path, timestamp, requestId, null);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CampoErro(String campo, String mensagem) {}
}
