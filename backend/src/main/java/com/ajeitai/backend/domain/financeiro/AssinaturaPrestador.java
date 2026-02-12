package com.ajeitai.backend.domain.financeiro;

import com.ajeitai.backend.domain.prestador.Prestador;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "assinaturas_prestador", indexes = {
        @Index(name = "idx_assinatura_prestador", columnList = "prestador_id"),
        @Index(name = "idx_assinatura_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssinaturaPrestador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestador_id", nullable = false)
    private Prestador prestador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAssinatura status;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(name = "ultimo_pagamento_em")
    private LocalDateTime ultimoPagamentoEm;

    /** Id da cobrança de assinatura na AbacatePay (bill_xxx). */
    @Column(name = "billing_id", length = 100)
    private String billingId;

    /** Valor atual da assinatura (permite reajustes futuros). */
    @Column(name = "valor_atual", precision = 10, scale = 2, nullable = false)
    private BigDecimal valorAtual;
}

