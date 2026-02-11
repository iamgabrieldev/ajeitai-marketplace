package com.ajeitai.backend.service;

import com.ajeitai.backend.domain.cliente.Cliente;
import com.ajeitai.backend.domain.cliente.DadosAtualizacaoCliente;
import com.ajeitai.backend.domain.cliente.DadosCadastroCliente;
import com.ajeitai.backend.domain.endereco.DadosEndereco;
import com.ajeitai.backend.domain.endereco.Endereco;
import com.ajeitai.backend.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClienteServiceTest {

    private ClienteRepository clienteRepository;
    private ClienteService clienteService;

    @BeforeEach
    void setup() {
        clienteRepository = mock(ClienteRepository.class);
        clienteService = new ClienteService(clienteRepository);
    }

    @Test
    void vincularQuandoNaoExiste_criaCliente() {
        DadosEndereco endereco = new DadosEndereco("Rua A", "Centro", "12345678", "Cidade", "UF", null, "10", -10.0, -10.0);
        DadosCadastroCliente dados = new DadosCadastroCliente("Joao", "joao@email.com", "11999999999", "12345678909", endereco);

        when(clienteRepository.findByKeycloakId("kc-1")).thenReturn(Optional.empty());
        when(clienteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Cliente cliente = clienteService.vincular("kc-1", "kc@email.com", dados);

        assertThat(cliente.getNome()).isEqualTo("Joao");
        assertThat(cliente.getEmail()).isEqualTo("kc@email.com");
        assertThat(cliente.getEndereco()).isNotNull();
    }

    @Test
    void vincularQuandoExiste_retornaExistente() {
        Cliente existente = Cliente.builder().id(1L).keycloakId("kc-1").nome("Existente").build();
        when(clienteRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(existente));

        Cliente cliente = clienteService.vincular("kc-1", "kc@email.com",
                new DadosCadastroCliente("Outro", "o@email.com", "11999999999", "12345678909",
                        new DadosEndereco("Rua A", "Centro", "12345678", "Cidade", "UF", null, "10", null, null)));

        assertThat(cliente.getNome()).isEqualTo("Existente");
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void buscarPorKeycloakIdQuandoNaoExiste_lancaErro() {
        when(clienteRepository.findByKeycloakId("kc-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.buscarPorKeycloakId("kc-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cliente não encontrado");
    }

    @Test
    void atualizarCliente_atualizaCampos() {
        Cliente cliente = Cliente.builder()
                .id(1L)
                .keycloakId("kc-1")
                .nome("Joao")
                .telefone("11999999999")
                .endereco(new Endereco("Rua A", "Centro", "12345678", "10", null, "Cidade", "UF", null, null))
                .build();
        when(clienteRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DadosAtualizacaoCliente dados = new DadosAtualizacaoCliente(
                "Joao Silva",
                "11888888888",
                new DadosEndereco("Rua B", "Centro", "12345678", "Cidade", "UF", null, "20", -10.0, -10.0)
        );

        Cliente atualizado = clienteService.atualizar("kc-1", dados);

        assertThat(atualizado.getNome()).isEqualTo("Joao Silva");
        assertThat(atualizado.getTelefone()).isEqualTo("11888888888");
        assertThat(atualizado.getEndereco().getLogradouro()).isEqualTo("Rua B");
    }

    @Test
    void atualizarAvatar_atualizaCampo() {
        Cliente cliente = Cliente.builder().id(1L).keycloakId("kc-1").build();
        when(clienteRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Cliente atualizado = clienteService.atualizarAvatar("kc-1", "cliente-1/avatar.png");

        assertThat(atualizado.getAvatarUrl()).isEqualTo("cliente-1/avatar.png");
    }
}
