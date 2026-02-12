package com.ajeitai.backend.domain.financeiro;

import com.ajeitai.backend.domain.agendamento.Agendamento;
import com.ajeitai.backend.domain.pagamento.Pagamento;
import com.ajeitai.backend.domain.prestador.Prestador;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transacoes", indexes = {
        @Index(name = "idx_wallet_trans_prestador", columnList = "prestador_id"),
        @Index(name = "idx_wallet_trans_tipo", columnList = "tipo"),
        @Index(name = "idx_wallet_trans_data", columnList = "criado_em")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransacaoWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestador_id", nullable = false)
    private Prestador prestador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoTransacaoWallet tipo;

    @Column(name = "valor_bruto", precision = 12, scale = 2, nullable = false)
    private BigDecimal valorBruto;

    @Column(name = "taxa_plataforma", precision = 12, scale = 2)
    private BigDecimal taxaPlataforma;

    @Column(name = "valor_liquido", precision = 12, scale = 2, nullable = false)
    private BigDecimal valorLiquido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id")
    private Agendamento agendamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pagamento_id")
    private Pagamento pagamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saque_id")
    private SaquePrestador saque;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }
}

