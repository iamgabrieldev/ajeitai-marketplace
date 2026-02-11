package com.ajeitai.backend.domain.catalogo;

public record PortfolioPublicoItem(
        Long id,
        String titulo,
        String descricao,
        String imagemUrl
) {
}
