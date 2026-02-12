package com.ajeitai.backend.domain.financeiro;

import com.ajeitai.backend.domain.prestador.Prestador;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "saques_prestador", indexes = {
        @Index(name = "idx_saque_prestador", columnList = "prestador_id"),
        @Index(name = "idx_saque_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaquePrestador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestador_id", nullable = false)
    private Prestador prestador;

    @Column(name = "valor_solicitado", precision = 12, scale = 2, nullable = false)
    private BigDecimal valorSolicitado;

    @Column(name = "valor_liquido", precision = 12, scale = 2, nullable = false)
    private BigDecimal valorLiquido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusSaque status;

    @Column(name = "referencia_externa", length = 100)
    private String referenciaExterna;

    @Column(name = "solicitado_em", nullable = false)
    private LocalDateTime solicitadoEm;

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;

    @PrePersist
    public void prePersist() {
        if (solicitadoEm == null) {
            solicitadoEm = LocalDateTime.now();
        }
    }
}

