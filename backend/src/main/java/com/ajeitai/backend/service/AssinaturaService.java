package com.ajeitai.backend.service;

import com.ajeitai.backend.domain.financeiro.AssinaturaPrestador;
import com.ajeitai.backend.domain.financeiro.StatusAssinatura;
import com.ajeitai.backend.domain.prestador.Prestador;
import com.ajeitai.backend.integration.abacatepay.AbacatePayService;
import com.ajeitai.backend.repository.AssinaturaPrestadorRepository;
import com.ajeitai.backend.repository.WalletPrestadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AssinaturaService {

    private final AssinaturaPrestadorRepository assinaturaRepository;
    private final WalletPrestadorRepository walletRepository;
    private final PrestadorService prestadorService;
    private final AbacatePayService abacatePayService;

    @Value("${app.assinatura.prestador.valor-mensal:15.00}")
    private BigDecimal valorMensal;

    @Transactional(readOnly = true)
    public AssinaturaResumo obterStatusAssinatura(String keycloakId) {
        Prestador prestador = prestadorService.buscarPorKeycloakId(keycloakId);
        return assinaturaRepository.findFirstByPrestadorIdAndStatusOrderByDataFimDesc(prestador.getId(), StatusAssinatura.ATIVA)
                .map(a -> new AssinaturaResumo(a.getStatus(), a.getDataInicio(), a.getDataFim(), null))
                .orElseGet(() -> new AssinaturaResumo(StatusAssinatura.CANCELADA, null, null, null));
    }

    /**
     * Inicia ou renova a assinatura, criando uma cobrança na AbacatePay quando necessário.
     */
    @Transactional
    public AssinaturaResumo iniciarOuRenovarAssinatura(String keycloakId) {
        Prestador prestador = prestadorService.buscarPorKeycloakId(keycloakId);
        LocalDate hoje = LocalDate.now();

        // Se já houver uma assinatura ATIVA com dataFim futura, apenas retorna o status
        var ativaOpt = assinaturaRepository.findFirstByPrestadorIdAndStatusOrderByDataFimDesc(prestador.getId(), StatusAssinatura.ATIVA);
        if (ativaOpt.isPresent() && ativaOpt.get().getDataFim() != null && !ativaOpt.get().getDataFim().isBefore(hoje)) {
            AssinaturaPrestador a = ativaOpt.get();
            return new AssinaturaResumo(a.getStatus(), a.getDataInicio(), a.getDataFim(), null);
        }

        // Cria um novo registro de assinatura em estado ATRASADA aguardando pagamento
        AssinaturaPrestador assinatura = AssinaturaPrestador.builder()
                .prestador(prestador)
                .status(StatusAssinatura.ATRASADA)
                .dataInicio(hoje)
                .valorAtual(valorMensal != null ? valorMensal : BigDecimal.valueOf(15))
                .build();
        assinatura = assinaturaRepository.save(assinatura);

        String externalId = "sub-" + assinatura.getId();
        var billingResult = abacatePayService.createSubscriptionBilling(prestador, assinatura.getValorAtual(), externalId);
        if (billingResult != null) {
            assinatura.setBillingId(billingResult.billingId());
            assinaturaRepository.save( assinatura );
            return new AssinaturaResumo(assinatura.getStatus(), assinatura.getDataInicio(), assinatura.getDataFim(), billingResult.paymentUrl());
        }

        return new AssinaturaResumo(assinatura.getStatus(), assinatura.getDataInicio(), assinatura.getDataFim(), null);
    }

    /**
     * Processa o pagamento confirmado de uma assinatura via webhook.
     */
    @Transactional
    public void processarPagamentoAssinatura(long assinaturaId, String billingId) {
        AssinaturaPrestador assinatura = assinaturaRepository.findById(assinaturaId)
                .orElseThrow(() -> new IllegalArgumentException("Assinatura não encontrada para id=" + assinaturaId));

        LocalDate hoje = LocalDate.now();
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setDataInicio(hoje);
        assinatura.setDataFim(hoje.plusDays(30));
        assinatura.setUltimoPagamentoEm(LocalDateTime.now());
        assinatura.setBillingId(billingId);
        assinaturaRepository.save(assinatura);

        // Garante que o prestador tenha uma wallet inicializada
        walletRepository.findByPrestadorId(assinatura.getPrestador().getId())
                .orElseGet(() -> walletRepository.save(
                        com.ajeitai.backend.domain.financeiro.WalletPrestador.builder()
                                .prestador(assinatura.getPrestador())
                                .saldoDisponivel(BigDecimal.ZERO)
                                .build()
                ));
    }

    @Transactional(readOnly = true)
    public boolean prestadorComAssinaturaAtiva(String keycloakId) {
        Prestador prestador = prestadorService.buscarPorKeycloakId(keycloakId);
        LocalDate hoje = LocalDate.now();
        return assinaturaRepository.findFirstByPrestadorIdAndStatusOrderByDataFimDesc(prestador.getId(), StatusAssinatura.ATIVA)
                .filter(a -> a.getDataFim() != null && !a.getDataFim().isBefore(hoje))
                .isPresent();
    }

    public record AssinaturaResumo(
            StatusAssinatura status,
            LocalDate dataInicio,
            LocalDate dataFim,
            String paymentUrl
    ) {}
}

