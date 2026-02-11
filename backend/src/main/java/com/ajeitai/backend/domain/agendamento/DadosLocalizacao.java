package com.ajeitai.backend.domain.agendamento;

import jakarta.validation.constraints.NotNull;

public record DadosLocalizacao(
        @NotNull
        Double latitude,
        @NotNull
        Double longitude
) {
}
