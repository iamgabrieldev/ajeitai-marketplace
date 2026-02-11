package com.ajeitai.backend.domain.notificacao;

import jakarta.validation.constraints.NotBlank;

public record DadosTokenPush(
        @NotBlank
        String token,
        String plataforma
) {
}
