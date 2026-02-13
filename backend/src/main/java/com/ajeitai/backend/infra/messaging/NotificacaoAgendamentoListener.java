package com.ajeitai.backend.infra.messaging;

import com.ajeitai.backend.domain.agendamento.AgendamentoCriadoEvent;
import com.ajeitai.backend.infra.config.RabbitMQConfig;
import com.ajeitai.backend.repository.AgendamentoRepository;
import com.ajeitai.backend.service.NotificacaoPushService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificacaoAgendamentoListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoAgendamentoListener.class);

    private final NotificacaoPushService notificacaoPushService;
    private final AgendamentoRepository agendamentoRepository;

    @RabbitListener(queues = RabbitMQConfig.AGENDAMENTOS_CRIADOS_QUEUE)
    public void onAgendamentoCriado(AgendamentoCriadoEvent event) {
        agendamentoRepository.findById(event.agendamentoId())
                .ifPresentOrElse(
                        agendamento -> {
                            notificacaoPushService.notificarNovoAgendamento(agendamento);
                            log.info("Notificação de novo agendamento enviada para agendamento {}", agendamento.getId());
                        },
                        () -> log.warn("Agendamento com id {} não encontrado ao processar AgendamentoCriadoEvent",
                                event.agendamentoId())
                );
    }
}


