package com.ajeitai.backend.integration.abacatepay;

import com.ajeitai.backend.domain.agendamento.Agendamento;
import com.ajeitai.backend.domain.cliente.Cliente;
import com.ajeitai.backend.domain.prestador.Prestador;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Integração com a API AbacatePay para criação de cobranças (links de pagamento PIX).
 * A cada cobrança são enviados o <strong>Cliente</strong> (criado/vinculado na AbacatePay) e um
 * <strong>Produto</strong> (atendimento). Cupons podem ser habilitados via configuração.
 *
 * @see <a href="https://docs.abacatepay.com/">Documentação AbacatePay</a>
 */
@Service
@RequiredArgsConstructor
public class AbacatePayService {

    private static final Logger log = LoggerFactory.getLogger(AbacatePayService.class);
    private static final String BILLING_CREATE_PATH = "/billing/create";

    private final AbacatePayProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Cria uma cobrança na AbacatePay para o agendamento. Envia sempre os dados do
     * <strong>cliente</strong> da compra (a AbacatePay cria ou vincula o cliente).
     * Retorna a URL de pagamento e o id da cobrança. Se a API key não estiver configurada, retorna null (modo mock).
     */
    public BillingResult createBilling(Agendamento agendamento) {
        if (!properties.isEnabled()) {
            log.warn("AbacatePay API key não configurada. Configure app.abacatepay.api-key para gerar links reais.");
            return null;
        }

        Cliente cliente = agendamento.getCliente();
        Prestador prestador = agendamento.getPrestador();
        BigDecimal valor = agendamento.getValorServico();
        if (valor == null || valor.compareTo(BigDecimal.ONE) < 0) {
            log.warn("Agendamento sem valor de serviço definido; usando 1 BRL para AbacatePay.");
            valor = BigDecimal.ONE;
        }
        int priceCents = valor.multiply(BigDecimal.valueOf(100)).intValue();
        if (priceCents < 100) {
            priceCents = 100; // mínimo 1 BRL
        }

        String externalId = "ag-" + agendamento.getId();
        String returnUrl = properties.getFrontendBaseUrl().replaceAll("/$", "") + "/cliente/agendamentos";
        String completionUrl = returnUrl;

        AbacatePayBillingRequest.Product product = new AbacatePayBillingRequest.Product(
                externalId,
                "Atendimento - " + (prestador.getNomeFantasia() != null ? prestador.getNomeFantasia() : "Prestador"),
                "Agendamento " + agendamento.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + " - " + (prestador.getNomeFantasia() != null ? prestador.getNomeFantasia() : ""),
                1,
                priceCents
        );

        AbacatePayBillingRequest.Customer customer = new AbacatePayBillingRequest.Customer(
                cliente.getNome() != null ? cliente.getNome() : "Cliente",
                normalizarCelular(cliente.getTelefone()),
                cliente.getEmail() != null ? cliente.getEmail() : "",
                normalizarCpf(cliente.getCpf())
        );

        List<String> coupons = properties.getCouponCodes().isEmpty() ? null : new ArrayList<>(properties.getCouponCodes());
        if (coupons != null && coupons.size() > 50) {
            coupons.subList(50, coupons.size()).clear();
        }

        AbacatePayBillingRequest request = new AbacatePayBillingRequest(
                AbacatePayBillingRequest.FREQUENCY_ONE_TIME,
                List.of(AbacatePayBillingRequest.METHOD_PIX),
                List.of(product),
                returnUrl,
                completionUrl,
                customer,
                externalId,
                properties.isAllowCoupons() ? true : null,
                coupons
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        try {
            ResponseEntity<AbacatePayBillingResponse> response = restTemplate.exchange(
                    properties.getBaseUrl().replaceAll("/$", "") + BILLING_CREATE_PATH,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    AbacatePayBillingResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().data() != null) {
                AbacatePayBillingResponse.Data data = response.getBody().data();
                return new BillingResult(data.id(), data.url());
            }
        } catch (Exception e) {
            log.error("Erro ao criar cobrança AbacatePay para agendamento {}", agendamento.getId(), e);
        }
        return null;
    }

    private static String normalizarCelular(String telefone) {
        if (telefone == null) return "00000000000";
        return telefone.replaceAll("\\D", "");
    }

    private static String normalizarCpf(String cpf) {
        if (cpf == null) return "00000000000";
        return cpf.replaceAll("\\D", "");
    }

    public record BillingResult(String billingId, String paymentUrl) {}
}
