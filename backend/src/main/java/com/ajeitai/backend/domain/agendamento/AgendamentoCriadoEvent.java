package com.ajeitai.backend.domain.agendamento;

import java.time.LocalDateTime;

/**
 * Evento de domínio disparado quando um agendamento é criado com sucesso.
 * Usado para propagar efeitos colaterais via mensageria (RabbitMQ).
 */
public record AgendamentoCriadoEvent(
        Long agendamentoId,
        Long clienteId,
        Long prestadorId,
        LocalDateTime dataHora
) {
}


