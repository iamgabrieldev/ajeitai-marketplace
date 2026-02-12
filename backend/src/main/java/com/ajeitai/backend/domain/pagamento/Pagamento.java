package com.ajeitai.backend.domain.pagamento;

import com.ajeitai.backend.domain.agendamento.Agendamento;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pagamentos", indexes = {
        @Index(name = "idx_pagamento_agendamento", columnList = "agendamento_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pagamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id", nullable = false, unique = true)
    @JsonIgnore
    private Agendamento agendamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPagamento status;

    @Column(name = "link_pagamento")
    private String linkPagamento;

    /** Id da cobrança na AbacatePay (bill_xxx). */
    @Column(name = "billing_id", length = 100)
    private String billingId;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "confirmado_em")
    private LocalDateTime confirmadoEm;

    @PrePersist
    public void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }

    public void confirmar() {
        this.status = StatusPagamento.CONFIRMADO;
        this.confirmadoEm = LocalDateTime.now();
    }
}
