package com.ajeitai.backend.domain.agendamento;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ajeitai.backend.domain.cliente.Cliente;
import com.ajeitai.backend.domain.endereco.Endereco;
import com.ajeitai.backend.domain.prestador.Prestador;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "agendamentos", indexes = {
        @Index(name = "idx_agendamento_cliente", columnList = "cliente_id"),
        @Index(name = "idx_agendamento_prestador", columnList = "prestador_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agendamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @JsonIgnore
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestador_id", nullable = false)
    @JsonIgnore
    private Prestador prestador;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAgendamento status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormaPagamento formaPagamento;

    @Column(name = "valor_servico", precision = 10, scale = 2)
    private BigDecimal valorServico;

    private String observacao;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "logradouro", column = @Column(name = "end_logradouro")),
            @AttributeOverride(name = "bairro", column = @Column(name = "end_bairro")),
            @AttributeOverride(name = "cep", column = @Column(name = "end_cep")),
            @AttributeOverride(name = "numero", column = @Column(name = "end_numero")),
            @AttributeOverride(name = "complemento", column = @Column(name = "end_complemento")),
            @AttributeOverride(name = "cidade", column = @Column(name = "end_cidade")),
            @AttributeOverride(name = "uf", column = @Column(name = "end_uf")),
            @AttributeOverride(name = "latitude", column = @Column(name = "end_latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "end_longitude"))
    })
    private Endereco endereco;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "confirmado_em")
    private LocalDateTime confirmadoEm;

    @Column(name = "checkin_em")
    private LocalDateTime checkinEm;

    @Column(name = "checkout_em")
    private LocalDateTime checkoutEm;

    @Column(name = "checkin_latitude")
    private Double checkinLatitude;

    @Column(name = "checkin_longitude")
    private Double checkinLongitude;

    @Column(name = "checkout_latitude")
    private Double checkoutLatitude;

    @Column(name = "checkout_longitude")
    private Double checkoutLongitude;

    @Column(name = "foto_trabalho_url", length = 512)
    private String fotoTrabalhoUrl;

    /** Preenchido na listagem do cliente: se pode abrir o fluxo de avaliação (REALIZADO, sem avaliação, dentro do prazo). */
    @Transient
    @JsonProperty("podeFazerAvaliacao")
    private Boolean podeFazerAvaliacao;

    /** Preenchido na listagem do cliente: id da avaliação já feita, ou null. */
    @Transient
    @JsonProperty("avaliacaoId")
    private String avaliacaoId;

    @PrePersist
    public void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }

    public void aceitar() {
        this.status = StatusAgendamento.ACEITO;
    }

    public void confirmar() {
        this.status = StatusAgendamento.CONFIRMADO;
        this.confirmadoEm = LocalDateTime.now();
    }

    public void recusar() {
        this.status = StatusAgendamento.RECUSADO;
    }

    public void cancelar() {
        this.status = StatusAgendamento.CANCELADO;
    }

    public void marcarRealizado() {
        this.status = StatusAgendamento.REALIZADO;
    }

    public Long getClienteId() {
        return cliente != null ? cliente.getId() : null;
    }

    public Long getPrestadorId() {
        return prestador != null ? prestador.getId() : null;
    }

    public String getClienteNome() {
        return cliente != null ? cliente.getNome() : null;
    }

    public String getPrestadorNome() {
        return prestador != null ? prestador.getNomeFantasia() : null;
    }

    /** Keycloak ID do prestador; usado pelo chat interno (cliente abre conversa). */
    @JsonProperty("prestadorKeycloakId")
    public String getPrestadorKeycloakId() {
        return prestador != null ? prestador.getKeycloakId() : null;
    }
}
