package com.ajeitai.backend.integration.abacatepay;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resposta da API AbacatePay POST /billing/create.
 * @see <a href="https://docs.abacatepay.com/pages/payment/create.md">Criar uma nova Cobrança</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AbacatePayBillingResponse(
        Data data,
        Object error
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            String id,
            String url,
            String status
    ) {}
}
