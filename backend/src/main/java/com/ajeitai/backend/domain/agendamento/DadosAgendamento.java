package com.ajeitai.backend.domain.agendamento;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record DadosAgendamento(
        @NotNull
        Long prestadorId,
        @NotNull
        @Future(message = "A data/hora do agendamento deve ser no futuro")
        LocalDateTime dataHora,
        @NotNull
        FormaPagamento formaPagamento,
        String observacao
) {
}
