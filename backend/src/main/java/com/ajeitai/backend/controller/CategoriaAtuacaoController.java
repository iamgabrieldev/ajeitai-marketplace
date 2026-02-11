package com.ajeitai.backend.controller;

import com.ajeitai.backend.domain.prestador.CategoriaAtuacao;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CategoriaAtuacaoController {

    @GetMapping("/categorias-atuacao")
    public ResponseEntity<List<String>> listar() {
        List<String> categorias = Arrays.stream(CategoriaAtuacao.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(categorias);
    }
}
