package com.ajeitai.backend.domain.catalogo;

import com.ajeitai.backend.domain.prestador.CategoriaAtuacao;

import java.math.BigDecimal;
import java.util.List;

public record PrestadorPublicoDetalhe(
        Long id,
        String nomeFantasia,
        CategoriaAtuacao categoria,
        String cidade,
        String uf,
        BigDecimal valorServico,
        Double mediaAvaliacao,
        String avatarUrl,
        List<PortfolioPublicoItem> portfolio
) {
}
