package com.ajeitai.backend.controller;

import com.ajeitai.backend.domain.catalogo.PrestadorPublicoDetalhe;
import com.ajeitai.backend.domain.catalogo.PrestadorPublicoResumo;
import com.ajeitai.backend.domain.prestador.CategoriaAtuacao;
import com.ajeitai.backend.service.CatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService catalogoService;

    @GetMapping("/prestadores")
    public ResponseEntity<List<PrestadorPublicoResumo>> listarPrestadores(
            @RequestParam(required = false) CategoriaAtuacao categoria,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) String uf,
            @RequestParam(required = false) Double minAvaliacao,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude
    ) {
        return ResponseEntity.ok(catalogoService.listarPrestadores(categoria, cidade, uf, minAvaliacao, latitude, longitude));
    }

    @GetMapping("/prestadores/{id}")
    public ResponseEntity<PrestadorPublicoDetalhe> buscarPrestador(@PathVariable Long id) {
        return ResponseEntity.ok(catalogoService.buscarDetalhe(id));
    }
}
