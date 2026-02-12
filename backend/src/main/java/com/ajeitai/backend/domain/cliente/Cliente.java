package com.ajeitai.backend.domain.cliente;

import com.ajeitai.backend.domain.endereco.Endereco;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clientes", indexes = {
        @Index(name = "idx_cliente_keycloak", columnList = "keycloak_id", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "keycloak_id", nullable = false, updatable = false)
    private String keycloakId;
    private String cpf;

    @Column(nullable = false)
    private String nome;
    @Column(nullable = false, unique = true)

    private String email;

    private String telefone;
    private Boolean ativo;
    @Column(name = "avatar_url")
    private String avatarUrl;
    @Embedded
    Endereco endereco;

    public void desativar() {
        this.ativo = false;
    }

}
