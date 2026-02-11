package com.ajeitai.backend.service;

import com.ajeitai.backend.domain.cliente.Cliente;
import com.ajeitai.backend.domain.cliente.DadosAtualizacaoCliente;
import com.ajeitai.backend.domain.cliente.DadosCadastroCliente;
import com.ajeitai.backend.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @Transactional
    public Cliente vincular(String keycloakId, String email, DadosCadastroCliente dados) {
        return clienteRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    Cliente cliente = Cliente.builder()
                            .keycloakId(keycloakId)
                            .email(email != null ? email : dados.email())
                            .nome(dados.nome())
                            .telefone(dados.telefone())
                            .cpf(dados.cpf())
                            .ativo(true)
                            .endereco(dados.endereco() != null ? new com.ajeitai.backend.domain.endereco.Endereco(dados.endereco()) : null)
                            .build();
                    return clienteRepository.save(cliente);
                });
    }

    public Cliente buscarPorKeycloakId(String keycloakId) {
        return clienteRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado para o usuário logado."));
    }

    @Transactional
    public Cliente atualizar(String keycloakId, DadosAtualizacaoCliente dados) {
        Cliente cliente = buscarPorKeycloakId(keycloakId);
        if (dados.nome() != null && !dados.nome().isBlank()) {
            cliente.setNome(dados.nome());
        }
        if (dados.telefone() != null) {
            cliente.setTelefone(dados.telefone());
        }
        if (dados.endereco() != null && cliente.getEndereco() != null) {
            cliente.getEndereco().atualizarInformacoes(dados.endereco());
        } else if (dados.endereco() != null) {
            cliente.setEndereco(new com.ajeitai.backend.domain.endereco.Endereco(dados.endereco()));
        }
        return clienteRepository.save(cliente);
    }

    @Transactional
    public Cliente atualizarAvatar(String keycloakId, String avatarUrl) {
        Cliente cliente = buscarPorKeycloakId(keycloakId);
        cliente.setAvatarUrl(avatarUrl);
        return clienteRepository.save(cliente);
    }
}
