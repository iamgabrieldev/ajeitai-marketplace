package com.ajeitai.backend.domain.cliente;

import com.ajeitai.backend.domain.endereco.DadosEndereco;

public record DadosAtualizacaoCliente(
        String nome,
        String telefone,
        DadosEndereco endereco
) {
}