package com.ajeitai.backend.controller;

import com.ajeitai.backend.domain.agendamento.Agendamento;
import com.ajeitai.backend.domain.agendamento.StatusAgendamento;
import com.ajeitai.backend.domain.avaliacao.Avaliacao;
import com.ajeitai.backend.domain.avaliacao.DadosAvaliacao;
import com.ajeitai.backend.domain.cliente.Cliente;
import com.ajeitai.backend.domain.cliente.DadosAtualizacaoCliente;
import com.ajeitai.backend.domain.cliente.DadosCadastroCliente;
import com.ajeitai.backend.service.AgendamentoService;
import com.ajeitai.backend.service.ArmazenamentoMidiaService;
import com.ajeitai.backend.service.AvaliacaoService;
import com.ajeitai.backend.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('cliente')")
public class ClienteController {

    private final ClienteService clienteService;
    private final AgendamentoService agendamentoService;
    private final AvaliacaoService avaliacaoService;
    private final ArmazenamentoMidiaService armazenamentoMidiaService;

    @PostMapping("/vincular")
    public ResponseEntity<Cliente> vincular(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DadosCadastroCliente dados
    ) {
        String keycloakId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        Cliente cliente = clienteService.vincular(keycloakId, email, dados);
        return ResponseEntity.ok(cliente);
    }

    @GetMapping("/me")
    public ResponseEntity<Cliente> me(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        Cliente cliente = clienteService.buscarPorKeycloakId(keycloakId);
        return ResponseEntity.ok(cliente);
    }

    @PutMapping("/me")
    public ResponseEntity<Cliente> atualizarMe(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DadosAtualizacaoCliente dados
    ) {
        String keycloakId = jwt.getSubject();
        Cliente cliente = clienteService.atualizar(keycloakId, dados);
        return ResponseEntity.ok(cliente);
    }

    @GetMapping("/me/agendamentos")
    public ResponseEntity<List<Agendamento>> listarAgendamentos(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) StatusAgendamento status
    ) {
        String keycloakId = jwt.getSubject();
        List<Agendamento> agendamentos = agendamentoService.listarPorCliente(keycloakId, Optional.ofNullable(status));
        return ResponseEntity.ok(agendamentos);
    }

    @PostMapping("/me/avaliacoes/{agendamentoId}")
    public ResponseEntity<Avaliacao> avaliar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long agendamentoId,
            @Valid @RequestBody DadosAvaliacao dados
    ) {
        String keycloakId = jwt.getSubject();
        Avaliacao avaliacao = avaliacaoService.avaliar(keycloakId, agendamentoId, dados);
        return ResponseEntity.ok(avaliacao);
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<Cliente> atualizarAvatar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("arquivo") MultipartFile arquivo
    ) throws java.io.IOException {
        String keycloakId = jwt.getSubject();
        Cliente clienteAtual = clienteService.buscarPorKeycloakId(keycloakId);
        String caminho = armazenamentoMidiaService.salvar("cliente-" + clienteAtual.getId() + "/avatar", arquivo);
        Cliente cliente = clienteService.atualizarAvatar(keycloakId, caminho);
        return ResponseEntity.ok(cliente);
    }
}
