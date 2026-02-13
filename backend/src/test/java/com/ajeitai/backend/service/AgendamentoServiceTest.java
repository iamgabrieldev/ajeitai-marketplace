package com.ajeitai.backend.service;

import com.ajeitai.backend.domain.agendamento.Agendamento;
import com.ajeitai.backend.domain.agendamento.DadosAgendamento;
import com.ajeitai.backend.domain.agendamento.DadosLocalizacao;
import com.ajeitai.backend.domain.agendamento.Disponibilidade;
import com.ajeitai.backend.domain.agendamento.FormaPagamento;
import com.ajeitai.backend.domain.agendamento.StatusAgendamento;
import com.ajeitai.backend.domain.cliente.Cliente;
import com.ajeitai.backend.domain.endereco.Endereco;
import com.ajeitai.backend.domain.prestador.Prestador;
import com.ajeitai.backend.repository.AgendamentoRepository;
import com.ajeitai.backend.repository.DisponibilidadeRepository;
import com.ajeitai.backend.repository.PagamentoRepository;
import com.ajeitai.backend.repository.PrestadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.CannotAcquireLockException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AgendamentoServiceTest {

    private ClienteService clienteService;
    private PrestadorRepository prestadorRepository;
    private AgendamentoRepository agendamentoRepository;
    private DisponibilidadeRepository disponibilidadeRepository;
    private PagamentoRepository pagamentoRepository;
    private PagamentoService pagamentoService;
    private WalletService walletService;
    private NotificacaoPushService notificacaoPushService;
    private MensageriaService mensageriaService;
    private ArmazenamentoMidiaService armazenamentoMidiaService;
    private ApplicationEventPublisher eventPublisher;
    private AgendamentoService agendamentoService;

    @BeforeEach
    void setup() {
        clienteService = mock(ClienteService.class);
        prestadorRepository = mock(PrestadorRepository.class);
        agendamentoRepository = mock(AgendamentoRepository.class);
        disponibilidadeRepository = mock(DisponibilidadeRepository.class);
        pagamentoRepository = mock(PagamentoRepository.class);
        pagamentoService = mock(PagamentoService.class);
        walletService = mock(WalletService.class);
        notificacaoPushService = mock(NotificacaoPushService.class);
        mensageriaService = mock(MensageriaService.class);
        armazenamentoMidiaService = mock(ArmazenamentoMidiaService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        agendamentoService = new AgendamentoService(
                clienteService,
                prestadorRepository,
                agendamentoRepository,
                disponibilidadeRepository,
                pagamentoRepository,
                pagamentoService,
                walletService,
                notificacaoPushService,
                mensageriaService,
                armazenamentoMidiaService,
                eventPublisher
        );
    }

    @Test
    void criarAgendamentoValido_salvaERetorna() {
        Cliente cliente = Cliente.builder()
                .id(1L)
                .keycloakId("cliente-1")
                .nome("Cliente")
                .email("cliente@teste.com")
                .endereco(new Endereco("Rua A", "Bairro", "12345678", "10", null, "Cidade", "UF", -10.0, -10.0))
                .build();
        Prestador prestador = Prestador.builder()
                .id(2L)
                .keycloakId("prestador-1")
                .nomeFantasia("Prestador")
                .endereco(new Endereco("Rua B", "Bairro", "12345678", "20", null, "Cidade", "UF", -10.1, -10.1))
                .build();
        LocalDateTime dataHora = LocalDateTime.now().plusHours(2);
        DadosAgendamento dados = new DadosAgendamento(prestador.getId(), dataHora, FormaPagamento.ONLINE, "Observacao");

        when(clienteService.buscarPorKeycloakId("cliente-1")).thenReturn(cliente);
        when(prestadorRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(prestador));
        when(disponibilidadeRepository.findByPrestadorIdAndDiaSemanaOrderByHoraInicioAsc(eq(2L), any()))
                .thenReturn(List.of(Disponibilidade.builder()
                        .prestador(prestador)
                        .diaSemana(dataHora.getDayOfWeek().getValue())
                        .horaInicio(LocalTime.of(8, 0))
                        .horaFim(LocalTime.of(18, 0))
                        .build()));
        when(agendamentoRepository.existsByPrestadorIdAndDataHoraBetweenAndStatusIn(any(), any(), any(), any()))
                .thenReturn(false);
        when(agendamentoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Agendamento agendamento = agendamentoService.criar("cliente-1", dados);

        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.PENDENTE);
        assertThat(agendamento.getFormaPagamento()).isEqualTo(FormaPagamento.ONLINE);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void criarAgendamentoCidadeDiferente_lancaErro() {
        Cliente cliente = Cliente.builder()
                .id(1L)
                .keycloakId("cliente-1")
                .nome("Cliente")
                .email("cliente@teste.com")
                .endereco(new Endereco("Rua A", "Bairro", "12345678", "10", null, "CidadeA", "UF", -10.0, -10.0))
                .build();
        Prestador prestador = Prestador.builder()
                .id(2L)
                .keycloakId("prestador-1")
                .nomeFantasia("Prestador")
                .endereco(new Endereco("Rua B", "Bairro", "12345678", "20", null, "CidadeB", "UF", -10.1, -10.1))
                .build();
        LocalDateTime dataHora = LocalDateTime.now().plusHours(2);
        DadosAgendamento dados = new DadosAgendamento(prestador.getId(), dataHora, FormaPagamento.ONLINE, "Obs");

        when(clienteService.buscarPorKeycloakId("cliente-1")).thenReturn(cliente);
        when(prestadorRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(prestador));

        assertThatThrownBy(() -> agendamentoService.criar("cliente-1", dados))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cidade do cliente");
    }

    @Test
    void criarAgendamentoForaDisponibilidade_lancaErro() {
        Cliente cliente = Cliente.builder()
                .id(1L)
                .keycloakId("cliente-1")
                .nome("Cliente")
                .email("cliente@teste.com")
                .endereco(new Endereco("Rua A", "Bairro", "12345678", "10", null, "Cidade", "UF", -10.0, -10.0))
                .build();
        Prestador prestador = Prestador.builder()
                .id(2L)
                .keycloakId("prestador-1")
                .nomeFantasia("Prestador")
                .endereco(new Endereco("Rua B", "Bairro", "12345678", "20", null, "Cidade", "UF", -10.1, -10.1))
                .build();
        LocalDateTime dataHora = LocalDateTime.now().plusHours(2);
        DadosAgendamento dados = new DadosAgendamento(prestador.getId(), dataHora, FormaPagamento.ONLINE, "Obs");

        when(clienteService.buscarPorKeycloakId("cliente-1")).thenReturn(cliente);
        when(prestadorRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(prestador));
        when(disponibilidadeRepository.findByPrestadorIdAndDiaSemanaOrderByHoraInicioAsc(eq(2L), any()))
                .thenReturn(List.of(Disponibilidade.builder()
                        .prestador(prestador)
                        .diaSemana(dataHora.getDayOfWeek().getValue())
                        .horaInicio(LocalTime.of(8, 0))
                        .horaFim(LocalTime.of(9, 0))
                        .build()));

        assertThatThrownBy(() -> agendamentoService.criar("cliente-1", dados))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("disponibilidade");
    }

    @Test
    void criarAgendamentoComConflito_lancaErro() {
        Cliente cliente = Cliente.builder()
                .id(1L)
                .keycloakId("cliente-1")
                .nome("Cliente")
                .email("cliente@teste.com")
                .endereco(new Endereco("Rua A", "Bairro", "12345678", "10", null, "Cidade", "UF", -10.0, -10.0))
                .build();
        Prestador prestador = Prestador.builder()
                .id(2L)
                .keycloakId("prestador-1")
                .nomeFantasia("Prestador")
                .endereco(new Endereco("Rua B", "Bairro", "12345678", "20", null, "Cidade", "UF", -10.1, -10.1))
                .build();
        LocalDateTime dataHora = LocalDateTime.now().plusHours(2);
        DadosAgendamento dados = new DadosAgendamento(prestador.getId(), dataHora, FormaPagamento.ONLINE, "Obs");

        when(clienteService.buscarPorKeycloakId("cliente-1")).thenReturn(cliente);
        when(prestadorRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(prestador));
        when(disponibilidadeRepository.findByPrestadorIdAndDiaSemanaOrderByHoraInicioAsc(eq(2L), any()))
                .thenReturn(List.of(Disponibilidade.builder()
                        .prestador(prestador)
                        .diaSemana(dataHora.getDayOfWeek().getValue())
                        .horaInicio(LocalTime.of(8, 0))
                        .horaFim(LocalTime.of(18, 0))
                        .build()));
        when(agendamentoRepository.existsByPrestadorIdAndDataHoraBetweenAndStatusIn(any(), any(), any(), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> agendamentoService.criar("cliente-1", dados))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Já existe um agendamento");
    }

    @Test
    void criarAgendamentoSemAntecedencia_lancaErro() {
        Cliente cliente = Cliente.builder()
                .id(1L)
                .keycloakId("cliente-1")
                .nome("Cliente")
                .email("cliente@teste.com")
                .endereco(new Endereco("Rua A", "Bairro", "12345678", "10", null, "Cidade", "UF", -10.0, -10.0))
                .build();
        Prestador prestador = Prestador.builder()
                .id(2L)
                .keycloakId("prestador-1")
                .nomeFantasia("Prestador")
                .endereco(new Endereco("Rua B", "Bairro", "12345678", "20", null, "Cidade", "UF", -10.1, -10.1))
                .build();
        LocalDateTime dataHora = LocalDateTime.now().plusMinutes(10);
        DadosAgendamento dados = new DadosAgendamento(prestador.getId(), dataHora, FormaPagamento.ONLINE, null);

        when(clienteService.buscarPorKeycloakId("cliente-1")).thenReturn(cliente);
        when(prestadorRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(prestador));

        assertThatThrownBy(() -> agendamentoService.criar("cliente-1", dados))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("30 minutos");
    }

    @Test
    void aceitarAgendamentoDinheiro_confirmaAutomaticamente() {
        Prestador prestador = Prestador.builder().id(2L).keycloakId("prestador-1").build();
        Agendamento agendamento = Agendamento.builder()
                .id(10L)
                .prestador(prestador)
                .status(StatusAgendamento.PENDENTE)
                .formaPagamento(FormaPagamento.DINHEIRO)
                .build();

        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(prestadorRepository.findByKeycloakId("prestador-1")).thenReturn(Optional.of(prestador));
        when(pagamentoService.criarPagamento(agendamento)).thenReturn(
                com.ajeitai.backend.domain.pagamento.Pagamento.builder()
                        .agendamento(agendamento)
                        .status(com.ajeitai.backend.domain.pagamento.StatusPagamento.NAO_APLICAVEL)
                        .build()
        );
        when(agendamentoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Agendamento resultado = agendamentoService.aceitar(10L, "prestador-1");

        assertThat(resultado.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
    }

    @Test
    void aceitarAgendamentoDeOutroPrestador_lancaErro() {
        Prestador prestador = Prestador.builder().id(2L).keycloakId("prestador-1").build();
        Prestador outro = Prestador.builder().id(3L).keycloakId("prestador-2").build();
        Agendamento agendamento = Agendamento.builder()
                .id(10L)
                .prestador(prestador)
                .status(StatusAgendamento.PENDENTE)
                .formaPagamento(FormaPagamento.ONLINE)
                .build();

        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(prestadorRepository.findByKeycloakId("prestador-2")).thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> agendamentoService.aceitar(10L, "prestador-2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Somente o prestador");
    }

    @Test
    void recusarAgendamentoNaoPendente_lancaErro() {
        Prestador prestador = Prestador.builder().id(2L).keycloakId("prestador-1").build();
        Agendamento agendamento = Agendamento.builder()
                .id(10L)
                .prestador(prestador)
                .status(StatusAgendamento.ACEITO)
                .build();

        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(prestadorRepository.findByKeycloakId("prestador-1")).thenReturn(Optional.of(prestador));

        assertThatThrownBy(() -> agendamentoService.recusar(10L, "prestador-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Somente agendamentos pendentes");
    }

    @Test
    void cancelarAgendamentoDeOutroCliente_lancaErro() {
        Cliente cliente = Cliente.builder().id(1L).keycloakId("cliente-1").build();
        Cliente outro = Cliente.builder().id(2L).keycloakId("cliente-2").build();
        Agendamento agendamento = Agendamento.builder()
                .id(10L)
                .cliente(cliente)
                .status(StatusAgendamento.PENDENTE)
                .build();

        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(clienteService.buscarPorKeycloakId("cliente-2")).thenReturn(outro);

        assertThatThrownBy(() -> agendamentoService.cancelar(10L, "cliente-2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Somente o cliente");
    }

    @Test
    void confirmarPagamentoClienteNaoDono_lancaErro() {
        Cliente cliente = Cliente.builder().id(1L).keycloakId("cliente-1").build();
        Cliente outro = Cliente.builder().id(2L).keycloakId("cliente-2").build();
        Agendamento agendamento = Agendamento.builder()
                .id(10L)
                .cliente(cliente)
                .status(StatusAgendamento.ACEITO)
                .build();

        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(clienteService.buscarPorKeycloakId("cliente-2")).thenReturn(outro);

        assertThatThrownBy(() -> agendamentoService.confirmarPagamento(10L, "cliente-2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Somente o cliente");
    }

    @Test
    void checkinECheckoutFluxoCompleto() {
        Prestador prestador = Prestador.builder().id(2L).keycloakId("prestador-1").build();
        Agendamento agendamento = Agendamento.builder()
                .id(10L)
                .prestador(prestador)
                .status(StatusAgendamento.CONFIRMADO)
                .build();

        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(prestadorRepository.findByKeycloakId("prestador-1")).thenReturn(Optional.of(prestador));
        when(agendamentoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Agendamento comCheckin = agendamentoService.registrarCheckin(10L, "prestador-1", new DadosLocalizacao(-10.0, -10.0));
        Agendamento comCheckout = agendamentoService.registrarCheckout(10L, "prestador-1", new DadosLocalizacao(-10.1, -10.1));

        assertThat(comCheckin.getCheckinEm()).isNotNull();
        assertThat(comCheckout.getCheckoutEm()).isNotNull();
        assertThat(comCheckout.getStatus()).isEqualTo(StatusAgendamento.REALIZADO);
    }

    @Test
    void criarAgendamentoQuandoNaoConsegueLock_lancaErroDeConcorrencia() {
        Cliente cliente = Cliente.builder()
                .id(1L)
                .keycloakId("cliente-1")
                .nome("Cliente")
                .email("cliente@teste.com")
                .endereco(new Endereco("Rua A", "Bairro", "12345678", "10", null, "Cidade", "UF", -10.0, -10.0))
                .build();
        Prestador prestador = Prestador.builder()
                .id(2L)
                .keycloakId("prestador-1")
                .nomeFantasia("Prestador")
                .endereco(new Endereco("Rua B", "Bairro", "12345678", "20", null, "Cidade", "UF", -10.1, -10.1))
                .build();
        LocalDateTime dataHora = LocalDateTime.now().plusHours(2);
        DadosAgendamento dados = new DadosAgendamento(prestador.getId(), dataHora, FormaPagamento.ONLINE, "Obs");

        when(clienteService.buscarPorKeycloakId("cliente-1")).thenReturn(cliente);
        when(prestadorRepository.findByIdForUpdate(2L)).thenThrow(new CannotAcquireLockException("lock"));

        assertThatThrownBy(() -> agendamentoService.criar("cliente-1", dados))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sendo agendado por outro cliente");
    }
}
