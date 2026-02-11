package com.ajeitai.backend.service;

import com.ajeitai.backend.domain.agendamento.Agendamento;
import com.ajeitai.backend.domain.agendamento.Disponibilidade;
import com.ajeitai.backend.domain.agendamento.DadosDisponibilidade;
import com.ajeitai.backend.domain.agendamento.StatusAgendamento;
import com.ajeitai.backend.domain.prestador.DadosAtualizacaoPrestador;
import com.ajeitai.backend.domain.prestador.DadosCadastroPrestador;
import com.ajeitai.backend.domain.prestador.DashboardPrestador;
import com.ajeitai.backend.domain.prestador.DocumentoPrestador;
import com.ajeitai.backend.domain.prestador.Prestador;
import com.ajeitai.backend.repository.AgendamentoRepository;
import com.ajeitai.backend.repository.DisponibilidadeRepository;
import com.ajeitai.backend.repository.DocumentoPrestadorRepository;
import com.ajeitai.backend.repository.PrestadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrestadorService {

    private final PrestadorRepository prestadorRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final DisponibilidadeRepository disponibilidadeRepository;
    private final DocumentoPrestadorRepository documentoPrestadorRepository;
    private final ArmazenamentoDocumentoService armazenamentoDocumentoService;

    @Transactional
    public Prestador vincular(String keycloakId, String email, DadosCadastroPrestador dados) {
        return prestadorRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    Prestador prestador = Prestador.builder()
                            .keycloakId(keycloakId)
                            .nomeFantasia(dados.nomeFantasia())
                            .cnpj(dados.cnpj())
                            .categoria(dados.categoria())
                            .valorServico(dados.valorServico())
                            .email(dados.email() != null ? dados.email() : email)
                            .telefone(dados.telefone())
                            .ativo(true)
                            .endereco(dados.endereco() != null ? new com.ajeitai.backend.domain.endereco.Endereco(dados.endereco()) : null)
                            .build();
                    return prestadorRepository.save(prestador);
                });
    }

    public Prestador buscarPorKeycloakId(String keycloakId) {
        return prestadorRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new IllegalArgumentException("Prestador não encontrado para o usuário logado."));
    }

    @Transactional
    public Prestador atualizar(String keycloakId, DadosAtualizacaoPrestador dados) {
        Prestador prestador = buscarPorKeycloakId(keycloakId);
        if (dados.nomeFantasia() != null && !dados.nomeFantasia().isBlank()) {
            prestador.setNomeFantasia(dados.nomeFantasia());
        }
        if (dados.email() != null) {
            prestador.setEmail(dados.email());
        }
        if (dados.telefone() != null) {
            prestador.setTelefone(dados.telefone());
        }
        if (dados.categoria() != null) {
            prestador.setCategoria(dados.categoria());
        }
        if (dados.valorServico() != null) {
            prestador.setValorServico(dados.valorServico());
        }
        if (dados.endereco() != null && prestador.getEndereco() != null) {
            prestador.getEndereco().atualizarInformacoes(dados.endereco());
        } else if (dados.endereco() != null) {
            prestador.setEndereco(new com.ajeitai.backend.domain.endereco.Endereco(dados.endereco()));
        }
        return prestadorRepository.save(prestador);
    }

    @Transactional
    public Prestador atualizarAvatar(String keycloakId, String avatarUrl) {
        Prestador prestador = buscarPorKeycloakId(keycloakId);
        prestador.setAvatarUrl(avatarUrl);
        return prestadorRepository.save(prestador);
    }

    public List<Agendamento> listarSolicitacoes(String keycloakId, Optional<StatusAgendamento> status) {
        if (status.isPresent()) {
            return agendamentoRepository.findByPrestadorKeycloakIdAndStatusOrderByDataHoraDesc(keycloakId, status.get());
        }
        return agendamentoRepository.findByPrestadorKeycloakIdOrderByDataHoraDesc(keycloakId);
    }

    public List<Disponibilidade> listarDisponibilidade(String keycloakId) {
        Prestador prestador = buscarPorKeycloakId(keycloakId);
        return disponibilidadeRepository.findByPrestadorIdOrderByDiaSemanaAscHoraInicioAsc(prestador.getId());
    }

    @Transactional
    public List<Disponibilidade> salvarDisponibilidade(String keycloakId, List<DadosDisponibilidade> dados) {
        Prestador prestador = buscarPorKeycloakId(keycloakId);
        disponibilidadeRepository.deleteByPrestadorId(prestador.getId());
        if (dados == null || dados.isEmpty()) {
            return List.of();
        }
        List<Disponibilidade> lista = dados.stream()
                .map(d -> Disponibilidade.builder()
                        .prestador(prestador)
                        .diaSemana(d.diaSemana())
                        .horaInicio(d.horaInicio())
                        .horaFim(d.horaFim())
                        .build())
                .collect(Collectors.toList());
        return disponibilidadeRepository.saveAll(lista);
    }

    public List<DocumentoPrestador> listarDocumentos(String keycloakId) {
        Prestador prestador = buscarPorKeycloakId(keycloakId);
        return documentoPrestadorRepository.findByPrestadorIdOrderByIdDesc(prestador.getId());
    }

    @Transactional
    public DocumentoPrestador anexarDocumento(String keycloakId, MultipartFile arquivo, String descricao) throws IOException {
        Prestador prestador = buscarPorKeycloakId(keycloakId);
        String caminho = armazenamentoDocumentoService.salvar(prestador.getId(), arquivo);
        DocumentoPrestador doc = DocumentoPrestador.builder()
                .prestador(prestador)
                .nomeArquivo(arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename() : "documento")
                .contentType(arquivo.getContentType())
                .caminho(caminho)
                .descricao(descricao)
                .build();
        return documentoPrestadorRepository.save(doc);
    }

    @Transactional
    public void removerDocumento(String keycloakId, Long documentoId) throws IOException {
        Prestador prestador = buscarPorKeycloakId(keycloakId);
        DocumentoPrestador doc = documentoPrestadorRepository.findByIdAndPrestadorId(documentoId, prestador.getId())
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado ou não pertence ao prestador."));
        armazenamentoDocumentoService.excluir(doc.getCaminho());
        documentoPrestadorRepository.delete(doc);
    }

    public DocumentoPrestador buscarDocumento(String keycloakId, Long documentoId) {
        Prestador prestador = buscarPorKeycloakId(keycloakId);
        return documentoPrestadorRepository.findByIdAndPrestadorId(documentoId, prestador.getId())
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado ou não pertence ao prestador."));
    }

    public DashboardPrestador dashboard(String keycloakId) {
        LocalDate hoje = LocalDate.now();
        LocalDateTime inicio = hoje.withDayOfMonth(1).atStartOfDay();
        LocalDateTime fim = hoje.plusMonths(1).withDayOfMonth(1).atStartOfDay().minusSeconds(1);
        List<Agendamento> realizados = agendamentoRepository.findByPrestadorKeycloakIdAndStatusAndDataHoraBetween(
                keycloakId,
                StatusAgendamento.REALIZADO,
                inicio,
                fim
        );
        BigDecimal ganhoBruto = realizados.stream()
                .map(Agendamento::getValorServico)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal lucroLiquido = ganhoBruto; // placeholder sem taxas do sistema
        return new DashboardPrestador(realizados.size(), ganhoBruto, lucroLiquido);
    }
}
