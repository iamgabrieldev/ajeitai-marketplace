package com.ajeitai.backend.infra.messaging;

import com.ajeitai.backend.domain.agendamento.AgendamentoCriadoEvent;
import com.ajeitai.backend.infra.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AgendamentoCriadoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AgendamentoCriadoEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAgendamentoCriado(AgendamentoCriadoEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.AGENDAMENTOS_EXCHANGE,
                RabbitMQConfig.AGENDAMENTOS_CRIADOS_RK,
                event
        );
        log.info("Publicado AgendamentoCriadoEvent para agendamento {} na exchange {}",
                event.agendamentoId(), RabbitMQConfig.AGENDAMENTOS_EXCHANGE);
    }
}


