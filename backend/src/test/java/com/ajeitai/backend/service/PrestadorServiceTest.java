package com.ajeitai.backend.service;

import com.ajeitai.backend.domain.agendamento.Agendamento;
import com.ajeitai.backend.domain.agendamento.DadosDisponibilidade;
import com.ajeitai.backend.domain.agendamento.Disponibilidade;
import com.ajeitai.backend.domain.agendamento.StatusAgendamento;
import com.ajeitai.backend.domain.endereco.DadosEndereco;
import com.ajeitai.backend.domain.endereco.Endereco;
import com.ajeitai.backend.domain.prestador.*;
import com.ajeitai.backend.repository.AgendamentoRepository;
import com.ajeitai.backend.repository.DisponibilidadeRepository;
import com.ajeitai.backend.repository.DocumentoPrestadorRepository;
import com.ajeitai.backend.repository.PrestadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PrestadorServiceTest {

    private PrestadorRepository prestadorRepository;
    private AgendamentoRepository agendamentoRepository;
    private DisponibilidadeRepository disponibilidadeRepository;
    private DocumentoPrestadorRepository documentoPrestadorRepository;
    private ArmazenamentoDocumentoService armazenamentoDocumentoService;
    private PrestadorService prestadorService;

    @BeforeEach
    void setup() {
        prestadorRepository = mock(PrestadorRepository.class);
        agendamentoRepository = mock(AgendamentoRepository.class);
        disponibilidadeRepository = mock(DisponibilidadeRepository.class);
        documentoPrestadorRepository = mock(DocumentoPrestadorRepository.class);
        armazenamentoDocumentoService = mock(ArmazenamentoDocumentoService.class);
        prestadorService = new PrestadorService(
                prestadorRepository,
                agendamentoRepository,
                disponibilidadeRepository,
                documentoPrestadorRepository,
                armazenamentoDocumentoService
        );
    }

    @Test
    void vincularQuandoNaoExiste_criaPrestador() {
        DadosEndereco endereco = new DadosEndereco("Rua A", "Centro", "12345678", "Cidade", "UF", null, "10", -10.0, -10.0);
        DadosCadastroPrestador dados = new DadosCadastroPrestador(
                "Casa Limpa",
                "prestador@email.com",
                "11999999999",
                "12345678000199",
                CategoriaAtuacao.LIMPEZA,
                BigDecimal.valueOf(120),
                endereco
        );

        when(prestadorRepository.findByKeycloakId("kc-1")).thenReturn(Optional.empty());
        when(prestadorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Prestador prestador = prestadorService.vincular("kc-1", "kc@email.com", dados);

        assertThat(prestador.getNomeFantasia()).isEqualTo("Casa Limpa");
        assertThat(prestador.getEmail()).isEqualTo("prestador@email.com");
    }

    @Test
    void vincularQuandoExiste_retornaExistente() {
        Prestador existente = Prestador.builder().id(1L).keycloakId("kc-1").nomeFantasia("Existente").build();
        when(prestadorRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(existente));

        Prestador prestador = prestadorService.vincular("kc-1", "kc@email.com",
                new DadosCadastroPrestador("Outro", "o@email.com", "11999999999", "12345678000199",
                        CategoriaAtuacao.OUTROS, null,
                        new DadosEndereco("Rua A", "Centro", "12345678", "Cidade", "UF", null, "10", null, null)));

        assertThat(prestador.getNomeFantasia()).isEqualTo("Existente");
        verify(prestadorRepository, never()).save(any());
    }

    @Test
    void buscarPorKeycloakIdQuandoNaoExiste_lancaErro() {
        when(prestadorRepository.findByKeycloakId("kc-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> prestadorService.buscarPorKeycloakId("kc-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prestador não encontrado");
    }

    @Test
    void atualizarPrestador_atualizaCampos() {
        Prestador prestador = Prestador.builder()
                .id(1L)
                .keycloakId("kc-1")
                .nomeFantasia("Casa Limpa")
                .endereco(new Endereco("Rua A", "Centro", "12345678", "10", null, "Cidade", "UF", null, null))
                .build();
        when(prestadorRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(prestador));
        when(prestadorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DadosAtualizacaoPrestador dados = new DadosAtualizacaoPrestador(
                "Casa Limpa Premium",
                "novo@email.com",
                "11988888888",
                CategoriaAtuacao.LIMPEZA,
                BigDecimal.valueOf(150),
                new DadosEndereco("Rua B", "Centro", "12345678", "Cidade", "UF", null, "20", -10.0, -10.0)
        );

        Prestador atualizado = prestadorService.atualizar("kc-1", dados);

        assertThat(atualizado.getNomeFantasia()).isEqualTo("Casa Limpa Premium");
        assertThat(atualizado.getEmail()).isEqualTo("novo@email.com");
        assertThat(atualizado.getEndereco().getLogradouro()).isEqualTo("Rua B");
    }

    @Test
    void atualizarAvatar_atualizaCampo() {
        Prestador prestador = Prestador.builder().id(1L).keycloakId("kc-1").build();
        when(prestadorRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(prestador));
        when(prestadorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Prestador atualizado = prestadorService.atualizarAvatar("kc-1", "prestador-1/avatar.png");

        assertThat(atualizado.getAvatarUrl()).isEqualTo("prestador-1/avatar.png");
    }

    @Test
    void listarSolicitacoesComStatus_filtraCorretamente() {
        Prestador prestador = Prestador.builder().id(1L).keycloakId("kc-1").build();
        when(prestadorRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(prestador));
        when(agendamentoRepository.findByPrestadorIdAndStatusOrderByDataHoraDesc(1L, StatusAgendamento.PENDENTE))
                .thenReturn(List.of(Agendamento.builder().id(10L).status(StatusAgendamento.PENDENTE).build()));

        List<Agendamento> lista = prestadorService.listarSolicitacoes("kc-1", Optional.of(StatusAgendamento.PENDENTE));

        assertThat(lista).hasSize(1);
    }

    @Test
    void listarDisponibilidade_retornaLista() {
        Prestador prestador = Prestador.builder().id(1L).keycloakId("kc-1").build();
        when(prestadorRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(prestador));
        when(disponibilidadeRepository.findByPrestadorIdOrderByDiaSemanaAscHoraInicioAsc(1L))
                .thenReturn(List.of(Disponibilidade.builder().id(1L).build()));

        List<Disponibilidade> lista = prestadorService.listarDisponibilidade("kc-1");

        assertThat(lista).hasSize(1);
    }

    @Test
    void salvarDisponibilidadeListaVazia_removeETornaVazio() {
        Prestador prestador = Prestador.builder().id(1L).keycloakId("kc-1").build();
        when(prestadorRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(prestador));

        List<Disponibilidade> lista = prestadorService.salvarDisponibilidade("kc-1", List.of());

        assertThat(lista).isEmpty();
        verify(disponibilidadeRepository).deleteByPrestadorId(1L);
    }

    @Test
    void salvarDisponibilidadeComDados_persiste() {
        Prestador prestador = Prestador.builder().id(1L).keycloakId("kc-1").build();
        when(prestadorRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(prestador));
        when(disponibilidadeRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<DadosDisponibilidade> dados = List.of(
                new DadosDisponibilidade(1, LocalTime.of(8, 0), LocalTime.of(18, 0))
        );

        List<Disponibilidade> lista = prestadorService.salvarDisponibilidade("kc-1", dados);

        assertThat(lista).hasSize(1);
    }

    @Test
    void anexarDocumento_salvaDocumento() throws IOException {
        Prestador prestador = Prestador.builder().id(1L).keycloakId("kc-1").build();
        MultipartFile arquivo = mock(MultipartFile.class);
        when(prestadorRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(prestador));
        when(arquivo.getOriginalFilename()).thenReturn("doc.pdf");
        when(arquivo.getContentType()).thenReturn("application/pdf");
        when(armazenamentoDocumentoService.salvar(1L, arquivo)).thenReturn("prestador-1/doc.pdf");
        when(documentoPrestadorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentoPrestador doc = prestadorService.anexarDocumento("kc-1", arquivo, "CNPJ");

        assertThat(doc.getCaminho()).isEqualTo("prestador-1/doc.pdf");
        assertThat(doc.getDescricao()).isEqualTo("CNPJ");
    }

    @Test
    void removerDocumento_excluiArquivoEDocumento() throws IOException {
        Prestador prestador = Prestador.builder().id(1L).keycloakId("kc-1").build();
        DocumentoPrestador doc = DocumentoPrestador.builder().id(10L).prestador(prestador).caminho("prestador-1/doc.pdf").build();
        when(prestadorRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(prestador));
        when(documentoPrestadorRepository.findByIdAndPrestadorId(10L, 1L)).thenReturn(Optional.of(doc));

        prestadorService.removerDocumento("kc-1", 10L);

        verify(armazenamentoDocumentoService).excluir("prestador-1/doc.pdf");
        verify(documentoPrestadorRepository).delete(doc);
    }

    @Test
    void buscarDocumento_quandoNaoPertence_lancaErro() {
        Prestador prestador = Prestador.builder().id(1L).keycloakId("kc-1").build();
        when(prestadorRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(prestador));
        when(documentoPrestadorRepository.findByIdAndPrestadorId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> prestadorService.buscarDocumento("kc-1", 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Documento não encontrado");
    }

    @Test
    void dashboard_calculaTotaisDoMes() {
        Prestador prestador = Prestador.builder().id(1L).keycloakId("kc-1").build();
        when(prestadorRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(prestador));
        when(agendamentoRepository.findByPrestadorIdAndStatusAndDataHoraBetween(eq(1L), eq(StatusAgendamento.REALIZADO), any(), any()))
                .thenReturn(List.of(
                        Agendamento.builder().valorServico(BigDecimal.valueOf(100)).dataHora(LocalDateTime.now()).build(),
                        Agendamento.builder().valorServico(BigDecimal.valueOf(200)).dataHora(LocalDateTime.now()).build()
                ));

        DashboardPrestador dashboard = prestadorService.dashboard("kc-1");

        assertThat(dashboard.quantidadeTrabalhosMes()).isEqualTo(2);
        assertThat(dashboard.ganhoBrutoMes()).isEqualTo(BigDecimal.valueOf(300));
    }
}
