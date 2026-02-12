package com.ajeitai.backend.integration.abacatepay;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request para POST /billing/create (AbacatePay).
 * @see <a href="https://docs.abacatepay.com/pages/payment/create.md">Criar uma nova Cobrança</a>
 */
public record AbacatePayBillingRequest(
        String frequency,
        List<String> methods,
        List<Product> products,
        @JsonProperty("returnUrl") String returnUrl,
        @JsonProperty("completionUrl") String completionUrl,
        Customer customer,
        @JsonProperty("externalId") String externalId,
        @JsonProperty("allowCoupons") Boolean allowCoupons,
        @JsonProperty("coupons") List<String> coupons
) {
    public static final String FREQUENCY_ONE_TIME = "ONE_TIME";
    public static final String METHOD_PIX = "PIX";
    public static final String METHOD_CREDIT_CARD = "CREDIT_CARD";

    public record Product(
            @JsonProperty("externalId") String externalId,
            String name,
            String description,
            int quantity,
            int price
    ) {}

    public record Customer(
            String name,
            String cellphone,
            String email,
            @JsonProperty("taxId") String taxId
    ) {}
}
