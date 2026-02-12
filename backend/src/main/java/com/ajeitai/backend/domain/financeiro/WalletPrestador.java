package com.ajeitai.backend.domain.financeiro;

import com.ajeitai.backend.domain.prestador.Prestador;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "wallet_prestador", indexes = {
        @Index(name = "idx_wallet_prestador", columnList = "prestador_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletPrestador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestador_id", nullable = false, unique = true)
    private Prestador prestador;

    @Column(name = "saldo_disponivel", precision = 12, scale = 2, nullable = false)
    private BigDecimal saldoDisponivel;

    @Column(name = "data_ultimo_saque")
    private LocalDate dataUltimoSaque;

    @PrePersist
    public void prePersist() {
        if (saldoDisponivel == null) {
            saldoDisponivel = BigDecimal.ZERO;
        }
    }
}

