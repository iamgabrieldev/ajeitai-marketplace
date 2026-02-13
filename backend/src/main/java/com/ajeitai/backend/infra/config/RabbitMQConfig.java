package com.ajeitai.backend.infra.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchanges
    public static final String AGENDAMENTOS_EXCHANGE = "agendamentos.exchange";
    public static final String NOTIFICACOES_EXCHANGE = "notificacoes.exchange";

    // Filas principais
    public static final String AGENDAMENTOS_CRIADOS_QUEUE = "agendamentos.criados";
    public static final String AGENDAMENTOS_PAGOS_QUEUE = "agendamentos.pagos";
    public static final String NOTIFICACOES_PUSH_QUEUE = "notificacoes.push";

    // DLQs
    public static final String AGENDAMENTOS_CRIADOS_DLQ = "agendamentos.criados.dlq";
    public static final String AGENDAMENTOS_PAGOS_DLQ = "agendamentos.pagos.dlq";
    public static final String NOTIFICACOES_PUSH_DLQ = "notificacoes.push.dlq";

    // Routing keys
    public static final String AGENDAMENTOS_CRIADOS_RK = "agendamentos.criados";
    public static final String AGENDAMENTOS_PAGOS_RK = "agendamentos.pagos";
    public static final String NOTIFICACOES_PUSH_RK = "notificacoes.push";

    @Bean
    public DirectExchange agendamentosExchange() {
        return new DirectExchange(AGENDAMENTOS_EXCHANGE);
    }

    @Bean
    public DirectExchange notificacoesExchange() {
        return new DirectExchange(NOTIFICACOES_EXCHANGE);
    }

    // Filas principais com DLQ configurada
    @Bean
    public Queue agendamentosCriadosQueue() {
        return QueueBuilder.durable(AGENDAMENTOS_CRIADOS_QUEUE)
                .withArgument("x-dead-letter-exchange", AGENDAMENTOS_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", AGENDAMENTOS_CRIADOS_DLQ)
                .build();
    }

    @Bean
    public Queue agendamentosPagosQueue() {
        return QueueBuilder.durable(AGENDAMENTOS_PAGOS_QUEUE)
                .withArgument("x-dead-letter-exchange", AGENDAMENTOS_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", AGENDAMENTOS_PAGOS_DLQ)
                .build();
    }

    @Bean
    public Queue notificacoesPushQueue() {
        return QueueBuilder.durable(NOTIFICACOES_PUSH_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICACOES_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", NOTIFICACOES_PUSH_DLQ)
                .build();
    }

    // DLQs
    @Bean
    public Queue agendamentosCriadosDlq() {
        return QueueBuilder.durable(AGENDAMENTOS_CRIADOS_DLQ).build();
    }

    @Bean
    public Queue agendamentosPagosDlq() {
        return QueueBuilder.durable(AGENDAMENTOS_PAGOS_DLQ).build();
    }

    @Bean
    public Queue notificacoesPushDlq() {
        return QueueBuilder.durable(NOTIFICACOES_PUSH_DLQ).build();
    }

    // Bindings principais
    @Bean
    public Binding agendamentosCriadosBinding(Queue agendamentosCriadosQueue, DirectExchange agendamentosExchange) {
        return BindingBuilder.bind(agendamentosCriadosQueue)
                .to(agendamentosExchange)
                .with(AGENDAMENTOS_CRIADOS_RK);
    }

    @Bean
    public Binding agendamentosPagosBinding(Queue agendamentosPagosQueue, DirectExchange agendamentosExchange) {
        return BindingBuilder.bind(agendamentosPagosQueue)
                .to(agendamentosExchange)
                .with(AGENDAMENTOS_PAGOS_RK);
    }

    @Bean
    public Binding notificacoesPushBinding(Queue notificacoesPushQueue, DirectExchange notificacoesExchange) {
        return BindingBuilder.bind(notificacoesPushQueue)
                .to(notificacoesExchange)
                .with(NOTIFICACOES_PUSH_RK);
    }

    // Bindings DLQ (podem ir para a mesma exchange com outra routing key ou ficar somente como parking lot)
    @Bean
    public Binding agendamentosCriadosDlqBinding(Queue agendamentosCriadosDlq, DirectExchange agendamentosExchange) {
        return BindingBuilder.bind(agendamentosCriadosDlq)
                .to(agendamentosExchange)
                .with(AGENDAMENTOS_CRIADOS_DLQ);
    }

    @Bean
    public Binding agendamentosPagosDlqBinding(Queue agendamentosPagosDlq, DirectExchange agendamentosExchange) {
        return BindingBuilder.bind(agendamentosPagosDlq)
                .to(agendamentosExchange)
                .with(AGENDAMENTOS_PAGOS_DLQ);
    }

    @Bean
    public Binding notificacoesPushDlqBinding(Queue notificacoesPushDlq, DirectExchange notificacoesExchange) {
        return BindingBuilder.bind(notificacoesPushDlq)
                .to(notificacoesExchange)
                .with(NOTIFICACOES_PUSH_DLQ);
    }

    // Converter JSON para todos os templates
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}


