package com.ajeitai.backend.domain.agendamento;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * diaSemana: 1 = Segunda-feira, 7 = Domingo.
 */
public record DadosDisponibilidade(
        @NotNull
        @Min(1)
        @Max(7)
        Integer diaSemana,
        @NotNull
        LocalTime horaInicio,
        @NotNull
        LocalTime horaFim
) {
}
