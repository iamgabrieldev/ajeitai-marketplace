package com.ajeitai.backend.controller;

import com.ajeitai.backend.domain.agendamento.Agendamento;
import com.ajeitai.backend.domain.agendamento.Disponibilidade;
import com.ajeitai.backend.domain.agendamento.DadosDisponibilidade;
import com.ajeitai.backend.domain.agendamento.StatusAgendamento;
import com.ajeitai.backend.domain.notificacao.DadosTokenPush;
import com.ajeitai.backend.domain.prestador.DadosAtualizacaoPrestador;
import com.ajeitai.backend.domain.prestador.DadosCadastroPrestador;
import com.ajeitai.backend.domain.prestador.DashboardPrestador;
import com.ajeitai.backend.domain.prestador.DocumentoPrestador;
import com.ajeitai.backend.domain.prestador.Prestador;
import com.ajeitai.backend.domain.portfolio.PortfolioItem;
import com.ajeitai.backend.service.ArmazenamentoDocumentoService;
import com.ajeitai.backend.service.ArmazenamentoMidiaService;
import com.ajeitai.backend.service.NotificacaoService;
import com.ajeitai.backend.service.PortfolioService;
import com.ajeitai.backend.service.PrestadorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/prestadores")
@RequiredArgsConstructor
@PreAuthorize("hasRole('prestador')")
public class PrestadorController {

    private final PrestadorService prestadorService;
    private final ArmazenamentoDocumentoService armazenamentoDocumentoService;
    private final ArmazenamentoMidiaService armazenamentoMidiaService;
    private final PortfolioService portfolioService;
    private final NotificacaoService notificacaoService;

    @PostMapping("/vincular")
    public ResponseEntity<Prestador> vincular(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DadosCadastroPrestador dados
    ) {
        String keycloakId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        Prestador prestador = prestadorService.vincular(keycloakId, email, dados);
        return ResponseEntity.ok(prestador);
    }

    @GetMapping("/me")
    public ResponseEntity<Prestador> me(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        Prestador prestador = prestadorService.buscarPorKeycloakId(keycloakId);
        return ResponseEntity.ok(prestador);
    }

    @PutMapping("/me")
    public ResponseEntity<Prestador> atualizarMe(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DadosAtualizacaoPrestador dados
    ) {
        String keycloakId = jwt.getSubject();
        Prestador prestador = prestadorService.atualizar(keycloakId, dados);
        return ResponseEntity.ok(prestador);
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<Prestador> atualizarAvatar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("arquivo") MultipartFile arquivo
    ) throws java.io.IOException {
        String keycloakId = jwt.getSubject();
        Prestador prestadorAtual = prestadorService.buscarPorKeycloakId(keycloakId);
        String caminho = armazenamentoMidiaService.salvar("prestador-" + prestadorAtual.getId() + "/avatar", arquivo);
        Prestador prestador = prestadorService.atualizarAvatar(keycloakId, caminho);
        return ResponseEntity.ok(prestador);
    }

    @GetMapping("/me/solicitacoes")
    public ResponseEntity<List<Agendamento>> solicitacoes(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) StatusAgendamento status
    ) {
        String keycloakId = jwt.getSubject();
        List<Agendamento> agendamentos = prestadorService.listarSolicitacoes(keycloakId, Optional.ofNullable(status));
        return ResponseEntity.ok(agendamentos);
    }

    @GetMapping("/me/dashboard")
    public ResponseEntity<DashboardPrestador> dashboard(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        return ResponseEntity.ok(prestadorService.dashboard(keycloakId));
    }

    @GetMapping("/me/disponibilidade")
    public ResponseEntity<List<Disponibilidade>> listarDisponibilidade(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        List<Disponibilidade> lista = prestadorService.listarDisponibilidade(keycloakId);
        return ResponseEntity.ok(lista);
    }

    @PutMapping("/me/disponibilidade")
    public ResponseEntity<List<Disponibilidade>> salvarDisponibilidade(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody List<DadosDisponibilidade> dados
    ) {
        String keycloakId = jwt.getSubject();
        List<Disponibilidade> lista = prestadorService.salvarDisponibilidade(keycloakId, dados);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/me/documentos")
    public ResponseEntity<List<DocumentoPrestador>> listarDocumentos(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        List<DocumentoPrestador> docs = prestadorService.listarDocumentos(keycloakId);
        return ResponseEntity.ok(docs);
    }

    @GetMapping("/me/portfolio")
    public ResponseEntity<List<PortfolioItem>> listarPortfolio(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        Prestador prestador = prestadorService.buscarPorKeycloakId(keycloakId);
        return ResponseEntity.ok(portfolioService.listarPorPrestador(prestador.getId()));
    }

    @PostMapping(value = "/me/portfolio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PortfolioItem> adicionarPortfolio(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam("titulo") String titulo,
            @RequestParam(value = "descricao", required = false) String descricao
    ) throws java.io.IOException {
        String keycloakId = jwt.getSubject();
        PortfolioItem item = portfolioService.adicionar(keycloakId, titulo, descricao, arquivo);
        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/me/portfolio/{id}")
    public ResponseEntity<Void> removerPortfolio(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        String keycloakId = jwt.getSubject();
        portfolioService.remover(keycloakId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/me/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoPrestador> anexarDocumento(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam(value = "descricao", required = false) String descricao
    ) throws java.io.IOException {
        String keycloakId = jwt.getSubject();
        DocumentoPrestador doc = prestadorService.anexarDocumento(keycloakId, arquivo, descricao);
        return ResponseEntity.ok(doc);
    }

    @DeleteMapping("/me/documentos/{id}")
    public ResponseEntity<Void> removerDocumento(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) throws java.io.IOException {
        String keycloakId = jwt.getSubject();
        prestadorService.removerDocumento(keycloakId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/documentos/{id}/download")
    public ResponseEntity<Resource> downloadDocumento(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) throws java.io.IOException {
        String keycloakId = jwt.getSubject();
        DocumentoPrestador doc = prestadorService.buscarDocumento(keycloakId, id);
        Resource resource = armazenamentoDocumentoService.obterRecurso(doc.getCaminho());
        if (resource == null || !resource.exists() || !resource.isReadable()) {
            throw new IllegalArgumentException("Arquivo não encontrado.");
        }
        String nomeArquivo = URLEncoder.encode(doc.getNomeArquivo() != null ? doc.getNomeArquivo() : "documento", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getContentType() != null ? doc.getContentType() : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                .body(resource);
    }

    @PostMapping("/me/notificacoes/token")
    public ResponseEntity<Void> registrarToken(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DadosTokenPush dados
    ) {
        String keycloakId = jwt.getSubject();
        notificacaoService.registrarToken(keycloakId, dados);
        return ResponseEntity.ok().build();
    }
}
