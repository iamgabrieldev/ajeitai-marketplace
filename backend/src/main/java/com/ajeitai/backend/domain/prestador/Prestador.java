package com.ajeitai.backend.domain.prestador;

import com.ajeitai.backend.domain.endereco.Endereco;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "prestadores", indexes = {
        @Index(name = "idx_prestador_keycloak", columnList = "keycloak_id", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Prestador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "keycloak_id", nullable = false, updatable = false)
    private String keycloakId;
    private String nomeFantasia;
    private String cnpj;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private CategoriaAtuacao categoria;
    private String email;
    private String telefone;
    private Boolean ativo;
    @Column(name = "avatar_url")
    private String avatarUrl;

    /**
     * Valor cobrado pelo trabalho (ex.: valor por hora ou por serviço).
     */
    @Column(name = "valor_servico", precision = 10, scale = 2)
    private BigDecimal valorServico;

    @Embedded
    private Endereco endereco;

    public void desativar() {
        this.ativo = false;
    }
}
