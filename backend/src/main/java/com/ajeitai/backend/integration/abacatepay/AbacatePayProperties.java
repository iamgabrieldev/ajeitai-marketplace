package com.ajeitai.backend.integration.abacatepay;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.abacatepay")
public class AbacatePayProperties {

    /** Chave de API (Bearer). Obrigatória para criar cobranças. */
    private String apiKey = "";

    /** URL base da API. Default: https://api.abacatepay.com/v1 */
    private String baseUrl = "https://api.abacatepay.com/v1";

    /** Secret configurado no webhook para validar requisições (query param webhookSecret). */
    private String webhookSecret = "";

    /** Base URL do frontend para returnUrl e completionUrl (ex: https://meuapp.com). */
    private String frontendBaseUrl = "http://localhost:3000";

    /** Se true, permite uso de cupons na página de pagamento. */
    private boolean allowCoupons = false;

    /** Códigos de cupom disponíveis para a cobrança (criados no dashboard AbacatePay). Máx. 50. */
    private List<String> couponCodes = List.of();

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public boolean isAllowCoupons() {
        return allowCoupons;
    }

    public void setAllowCoupons(boolean allowCoupons) {
        this.allowCoupons = allowCoupons;
    }

    public List<String> getCouponCodes() {
        return couponCodes != null ? couponCodes : List.of();
    }

    public void setCouponCodes(List<String> couponCodes) {
        this.couponCodes = couponCodes != null ? couponCodes : List.of();
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
