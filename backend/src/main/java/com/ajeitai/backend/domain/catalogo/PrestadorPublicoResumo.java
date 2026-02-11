package com.ajeitai.backend.domain.catalogo;

import com.ajeitai.backend.domain.prestador.CategoriaAtuacao;

import java.math.BigDecimal;

public record PrestadorPublicoResumo(
        Long id,
        String nomeFantasia,
        CategoriaAtuacao categoria,
        String cidade,
        String uf,
        BigDecimal valorServico,
        Double mediaAvaliacao,
        Long totalAvaliacoes,
        Double distanciaKm,
        Long totalServicos,
        String avatarUrl
) {
}
