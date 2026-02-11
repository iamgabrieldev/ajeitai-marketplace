package com.ajeitai.backend.service;

import com.ajeitai.backend.domain.agendamento.Agendamento;
import com.ajeitai.backend.domain.agendamento.StatusAgendamento;
import com.ajeitai.backend.domain.avaliacao.DadosAvaliacao;
import com.ajeitai.backend.domain.cliente.Cliente;
import com.ajeitai.backend.repository.AgendamentoRepository;
import com.ajeitai.backend.repository.AvaliacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AvaliacaoServiceTest {

    private AvaliacaoRepository avaliacaoRepository;
    private AgendamentoRepository agendamentoRepository;
    private ClienteService clienteService;
    private AvaliacaoService avaliacaoService;

    @BeforeEach
    void setup() {
        avaliacaoRepository = mock(AvaliacaoRepository.class);
        agendamentoRepository = mock(AgendamentoRepository.class);
        clienteService = mock(ClienteService.class);
        avaliacaoService = new AvaliacaoService(avaliacaoRepository, agendamentoRepository, clienteService);
    }

    @Test
    void avaliarAgendamentoRealizado_criaAvaliacao() {
        Cliente cliente = Cliente.builder().id(1L).keycloakId("cliente-1").build();
        Agendamento agendamento = Agendamento.builder()
                .id(10L)
                .cliente(cliente)
                .prestador(null)
                .status(StatusAgendamento.REALIZADO)
                .checkoutEm(LocalDateTime.now().minusDays(1))
                .build();

        when(clienteService.buscarPorKeycloakId("cliente-1")).thenReturn(cliente);
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(avaliacaoRepository.findByAgendamentoId(10L)).thenReturn(Optional.empty());
        when(avaliacaoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var avaliacao = avaliacaoService.avaliar("cliente-1", 10L, new DadosAvaliacao(5, "Ótimo"));

        assertThat(avaliacao.getNota()).isEqualTo(5);
    }

    @Test
    void avaliarAgendamentoNaoRealizado_lancaErro() {
        Cliente cliente = Cliente.builder().id(1L).keycloakId("cliente-1").build();
        Agendamento agendamento = Agendamento.builder()
                .id(10L)
                .cliente(cliente)
                .status(StatusAgendamento.ACEITO)
                .build();

        when(clienteService.buscarPorKeycloakId("cliente-1")).thenReturn(cliente);
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));

        assertThatThrownBy(() -> avaliacaoService.avaliar("cliente-1", 10L, new DadosAvaliacao(5, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("realizados");
    }

    @Test
    void avaliarAgendamentoJaAvaliado_lancaErro() {
        Cliente cliente = Cliente.builder().id(1L).keycloakId("cliente-1").build();
        Agendamento agendamento = Agendamento.builder()
                .id(10L)
                .cliente(cliente)
                .status(StatusAgendamento.REALIZADO)
                .checkoutEm(LocalDateTime.now())
                .build();

        when(clienteService.buscarPorKeycloakId("cliente-1")).thenReturn(cliente);
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(avaliacaoRepository.findByAgendamentoId(10L)).thenReturn(Optional.of(mock(com.ajeitai.backend.domain.avaliacao.Avaliacao.class)));

        assertThatThrownBy(() -> avaliacaoService.avaliar("cliente-1", 10L, new DadosAvaliacao(5, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já foi avaliado");
    }

    @Test
    void avaliarAgendamentoForaPrazo_lancaErro() {
        Cliente cliente = Cliente.builder().id(1L).keycloakId("cliente-1").build();
        Agendamento agendamento = Agendamento.builder()
                .id(10L)
                .cliente(cliente)
                .status(StatusAgendamento.REALIZADO)
                .checkoutEm(LocalDateTime.now().minusDays(10))
                .build();

        when(clienteService.buscarPorKeycloakId("cliente-1")).thenReturn(cliente);
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(avaliacaoRepository.findByAgendamentoId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> avaliacaoService.avaliar("cliente-1", 10L, new DadosAvaliacao(5, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expirou");
    }

    @Test
    void avaliarAgendamentoDeOutroCliente_lancaErro() {
        Cliente cliente = Cliente.builder().id(1L).keycloakId("cliente-1").build();
        Cliente outro = Cliente.builder().id(2L).keycloakId("cliente-2").build();
        Agendamento agendamento = Agendamento.builder()
                .id(10L)
                .cliente(cliente)
                .status(StatusAgendamento.REALIZADO)
                .checkoutEm(LocalDateTime.now())
                .build();

        when(clienteService.buscarPorKeycloakId("cliente-2")).thenReturn(outro);
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));

        assertThatThrownBy(() -> avaliacaoService.avaliar("cliente-2", 10L, new DadosAvaliacao(5, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Somente o cliente");
    }
}
