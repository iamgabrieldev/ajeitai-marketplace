package com.ajeitai.backend.controller;

import com.ajeitai.backend.domain.agendamento.Agendamento;
import com.ajeitai.backend.domain.agendamento.StatusAgendamento;
import com.ajeitai.backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/visao-geral")
    public ResponseEntity<AdminService.VisaoGeralDto> visaoGeral() {
        return ResponseEntity.ok(adminService.visaoGeral());
    }

    @GetMapping("/prestadores")
    public ResponseEntity<List<AdminService.PrestadorAdminDto>> listarPrestadores() {
        return ResponseEntity.ok(adminService.listarPrestadores());
    }

    @GetMapping("/agendamentos")
    public ResponseEntity<List<Agendamento>> listarAgendamentos(
            @RequestParam(required = false) String status
    ) {
        StatusAgendamento statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = StatusAgendamento.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        return ResponseEntity.ok(adminService.listarAgendamentos(statusEnum));
    }

    @GetMapping("/pagamentos")
    public ResponseEntity<List<AdminService.PagamentoAdminDto>> listarPagamentos() {
        return ResponseEntity.ok(adminService.listarPagamentos());
    }

    @GetMapping("/saques")
    public ResponseEntity<List<AdminService.SaqueAdminDto>> listarSaques() {
        return ResponseEntity.ok(adminService.listarSaques());
    }
}
