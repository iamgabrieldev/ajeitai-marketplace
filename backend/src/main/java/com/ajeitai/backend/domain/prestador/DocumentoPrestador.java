package com.ajeitai.backend.domain.prestador;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "documentos_prestador", indexes = {
        @Index(name = "idx_documento_prestador_id", columnList = "prestador_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoPrestador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestador_id", nullable = false)
    private Prestador prestador;

    @Column(nullable = false)
    private String nomeArquivo;

    @Column(name = "content_type")
    private String contentType;

    /**
     * Caminho relativo ou identificador do arquivo (ex.: path local ou key em storage).
     */
    @Column(nullable = false)
    private String caminho;

    private String descricao;
}
