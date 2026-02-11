package com.ajeitai.backend.domain.prestador;

import com.ajeitai.backend.domain.endereco.DadosEndereco;

import java.math.BigDecimal;

public record DadosAtualizacaoPrestador(
        String nomeFantasia,
        String email,
        String telefone,
        CategoriaAtuacao categoria,
        BigDecimal valorServico,
        DadosEndereco endereco
) {
}
