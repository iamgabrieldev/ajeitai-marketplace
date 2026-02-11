package com.ajeitai.backend.controller;

import com.ajeitai.backend.domain.catalogo.PrestadorPublicoResumo;
import com.ajeitai.backend.domain.prestador.CategoriaAtuacao;
import com.ajeitai.backend.service.CatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prestadores")
@RequiredArgsConstructor
public class ListagemPrestadoresController {

    private final CatalogoService catalogoService;

    /**
     * Lista prestadores paginados para cliente/prestador (catálogo).
     * Query params: page (0-based), size, search, categoria, avaliacaoMin, orderBy, latitude, longitude.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('cliente','prestador')")
    public ResponseEntity<Page<PrestadorPublicoResumo>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Double avaliacaoMin,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude
    ) {
        CategoriaAtuacao cat = parseCategoria(categoria);
        Pageable pageable = PageRequest.of(page, size);
        Page<PrestadorPublicoResumo> result = catalogoService.listarPrestadoresPaginated(
                pageable, search, cat, avaliacaoMin, orderBy, latitude, longitude);
        return ResponseEntity.ok(result);
    }

    private static CategoriaAtuacao parseCategoria(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return CategoriaAtuacao.valueOf(valor.toUpperCase().replace("-", "_").replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
