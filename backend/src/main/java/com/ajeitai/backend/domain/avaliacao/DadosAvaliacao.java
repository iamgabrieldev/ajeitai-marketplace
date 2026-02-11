package com.ajeitai.backend.domain.avaliacao;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DadosAvaliacao(
        @NotNull
        @Min(1)
        @Max(5)
        Integer nota,
        String comentario
) {
}
