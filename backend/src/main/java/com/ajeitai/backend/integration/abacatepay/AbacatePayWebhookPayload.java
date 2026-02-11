package com.ajeitai.backend.integration.abacatepay;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Payload do webhook AbacatePay (ex: billing.paid).
 * @see <a href="https://docs.abacatepay.com/pages/webhooks.md">Webhooks</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AbacatePayWebhookPayload(
        String id,
        String event,
        Data data,
        Boolean devMode
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            Billing billing,
            Payment payment,
            PixQrCode pixQrCode
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Billing(
            String id,
            String status,
            List<Product> products
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Product(
            @JsonProperty("externalId") String externalId
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Payment(
            Integer amount,
            String method
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PixQrCode(
            String id,
            String status
    ) {}
}
