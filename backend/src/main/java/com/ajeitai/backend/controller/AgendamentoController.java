package com.ajeitai.backend.controller;

import com.ajeitai.backend.domain.agendamento.Agendamento;
import com.ajeitai.backend.domain.agendamento.DadosAgendamento;
import com.ajeitai.backend.domain.agendamento.DadosLocalizacao;
import com.ajeitai.backend.domain.agendamento.StatusAgendamento;
import com.ajeitai.backend.domain.pagamento.Pagamento;
import com.ajeitai.backend.repository.AvaliacaoRepository;
import com.ajeitai.backend.service.AgendamentoService;
import com.ajeitai.backend.service.PagamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/agendamentos")
@RequiredArgsConstructor
public class AgendamentoController {

    private final AgendamentoService agendamentoService;
    private final PagamentoService pagamentoService;
    private final AvaliacaoRepository avaliacaoRepository;

    @PostMapping
    @PreAuthorize("hasRole('cliente')")
    public ResponseEntity<Agendamento> criar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DadosAgendamento dados
    ) {
        String keycloakId = jwt.getSubject();
        Agendamento agendamento = agendamentoService.criar(keycloakId, dados);
        return ResponseEntity.ok(agendamento);
    }

    @GetMapping
    @PreAuthorize("hasRole('cliente')")
    public ResponseEntity<List<Agendamento>> listarMeus(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status
    ) {
        String keycloakId = jwt.getSubject();
        Optional<StatusAgendamento> statusEnum = mapStatusFromFrontend(status);
        List<Agendamento> agendamentos = statusEnum
                .map(s -> agendamentoService.listarPorCliente(keycloakId, Optional.of(s)))
                .orElseGet(() -> agendamentoService.listarPorCliente(keycloakId));
        LocalDateTime agora = LocalDateTime.now();
        for (Agendamento a : agendamentos) {
            if (a.getStatus() != StatusAgendamento.REALIZADO) {
                a.setPodeFazerAvaliacao(false);
                a.setAvaliacaoId(null);
                continue;
            }
            var avOpt = avaliacaoRepository.findByAgendamentoId(a.getId());
            if (avOpt.isPresent()) {
                a.setAvaliacaoId(String.valueOf(avOpt.get().getId()));
                a.setPodeFazerAvaliacao(false);
            } else {
                LocalDateTime ref = a.getCheckoutEm() != null ? a.getCheckoutEm() : a.getDataHora();
                boolean dentroPrazo = ref == null || ref.plusDays(7).isAfter(agora);
                a.setPodeFazerAvaliacao(dentroPrazo);
                a.setAvaliacaoId(null);
            }
        }
        return ResponseEntity.ok(agendamentos);
    }

    private static Optional<StatusAgendamento> mapStatusFromFrontend(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        return switch (value.toLowerCase()) {
            case "solicitado" -> Optional.of(StatusAgendamento.PENDENTE);
            case "agendado" -> Optional.of(StatusAgendamento.ACEITO);
            case "em_andamento" -> Optional.of(StatusAgendamento.CONFIRMADO);
            case "concluido" -> Optional.of(StatusAgendamento.REALIZADO);
            case "cancelado" -> Optional.of(StatusAgendamento.CANCELADO);
            case "recusado" -> Optional.of(StatusAgendamento.RECUSADO);
            default -> Optional.ofNullable(java.util.Arrays.stream(StatusAgendamento.values())
                    .filter(e -> e.name().equalsIgnoreCase(value))
                    .findFirst().orElse(null));
        };
    }

    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('cliente')")
    public ResponseEntity<Void> cancelar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        String keycloakId = jwt.getSubject();
        agendamentoService.cancelar(id, keycloakId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/aceitar")
    @PreAuthorize("hasRole('prestador')")
    public ResponseEntity<Void> aceitar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        String keycloakId = jwt.getSubject();
        agendamentoService.aceitar(id, keycloakId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/recusar")
    @PreAuthorize("hasRole('prestador')")
    public ResponseEntity<Void> recusar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        String keycloakId = jwt.getSubject();
        agendamentoService.recusar(id, keycloakId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/checkin")
    @PreAuthorize("hasRole('prestador')")
    public ResponseEntity<Void> checkin(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody DadosLocalizacao localizacao
    ) {
        String keycloakId = jwt.getSubject();
        agendamentoService.registrarCheckin(id, keycloakId, localizacao);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/checkout")
    @PreAuthorize("hasRole('prestador')")
    public ResponseEntity<Void> checkout(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody DadosLocalizacao localizacao
    ) {
        String keycloakId = jwt.getSubject();
        agendamentoService.registrarCheckout(id, keycloakId, localizacao);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{id}/checkout-com-foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('prestador')")
    public ResponseEntity<Void> checkoutComFoto(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestParam("latitude") double latitude,
            @RequestParam("longitude") double longitude,
            @RequestParam("foto") MultipartFile foto
    ) {
        if (foto == null || foto.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String keycloakId = jwt.getSubject();
        DadosLocalizacao localizacao = new DadosLocalizacao(latitude, longitude);
        agendamentoService.registrarCheckoutComFoto(id, keycloakId, localizacao, foto);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/confirmar-pagamento")
    @PreAuthorize("hasRole('cliente')")
    public ResponseEntity<Void> confirmarPagamento(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        String keycloakId = jwt.getSubject();
        agendamentoService.confirmarPagamento(id, keycloakId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pagamento")
    @PreAuthorize("hasRole('cliente')")
    public ResponseEntity<Pagamento> buscarPagamento(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        String keycloakId = jwt.getSubject();
        Agendamento agendamento = agendamentoService.buscarPorIdDoCliente(id, keycloakId);
        Pagamento pagamento = pagamentoService.criarPagamento(agendamento);
        return ResponseEntity.ok(pagamento);
    }
}
