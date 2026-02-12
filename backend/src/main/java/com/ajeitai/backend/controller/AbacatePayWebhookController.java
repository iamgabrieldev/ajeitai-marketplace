package com.ajeitai.backend.controller;

import com.ajeitai.backend.integration.abacatepay.AbacatePayProperties;
import com.ajeitai.backend.integration.abacatepay.AbacatePayWebhookPayload;
import com.ajeitai.backend.service.AgendamentoService;
import com.ajeitai.backend.service.AssinaturaService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Recebe webhooks da AbacatePay para confirmar pagamentos automaticamente.
 * @see <a href="https://docs.abacatepay.com/pages/webhooks.md">Webhooks AbacatePay</a>
 */
@RestController
@RequestMapping("/api/webhooks/abacatepay")
@RequiredArgsConstructor
public class AbacatePayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(AbacatePayWebhookController.class);
    private static final String EVENT_BILLING_PAID = "billing.paid";

    private final AbacatePayProperties properties;
    private final AgendamentoService agendamentoService;
    private final AssinaturaService assinaturaService;

    /**
     * AbacatePay chama esta URL com o secret em query: ?webhookSecret=xxx
     */
    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestParam(value = "webhookSecret", required = false) String webhookSecret,
            @RequestBody AbacatePayWebhookPayload payload
    ) {
        if (payload == null) {
            return ResponseEntity.badRequest().build();
        }

        if (properties.getWebhookSecret() != null && !properties.getWebhookSecret().isBlank()) {
            if (!properties.getWebhookSecret().equals(webhookSecret)) {
                log.warn("Webhook AbacatePay rejeitado: secret inválido ou ausente.");
                return ResponseEntity.status(401).build();
            }
        }

        if (EVENT_BILLING_PAID.equals(payload.event())) {
            return processBillingPaid(payload);
        }

        log.debug("Webhook AbacatePay ignorado: event={}", payload.event());
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<Void> processBillingPaid(AbacatePayWebhookPayload payload) {
        String externalId = null;
        if (payload.data() != null && payload.data().billing() != null && payload.data().billing().products() != null
                && !payload.data().billing().products().isEmpty()) {
            externalId = payload.data().billing().products().get(0).externalId();
        }
        if (externalId == null) {
            log.warn("Webhook billing.paid sem externalId: {}", payload);
            return ResponseEntity.ok().build();
        }

        try {
            if (externalId.startsWith("ag-")) {
                long agendamentoId = Long.parseLong(externalId.substring(3));
                agendamentoService.confirmarPagamentoPorIdAgendamento(agendamentoId);
                log.info("Pagamento confirmado via webhook AbacatePay para agendamento {}", agendamentoId);
            } else if (externalId.startsWith("sub-")) {
                long assinaturaId = Long.parseLong(externalId.substring(4));
                String billingId = payload.data() != null && payload.data().billing() != null
                        ? payload.data().billing().id()
                        : null;
                assinaturaService.processarPagamentoAssinatura(assinaturaId, billingId);
                log.info("Pagamento de assinatura confirmado via webhook AbacatePay para assinatura {}", assinaturaId);
            } else {
                log.warn("Webhook billing.paid com externalId desconhecido: {}", externalId);
            }
        } catch (NumberFormatException e) {
            log.warn("Webhook billing.paid com externalId inválido: {}", externalId);
        } catch (Exception e) {
            log.error("Erro ao processar billing.paid para externalId={}", externalId, e);
            return ResponseEntity.status(500).build();
        }

        return ResponseEntity.ok().build();
    }
}
