package com.ajeitai.backend.service;

import com.ajeitai.backend.domain.portfolio.PortfolioItem;
import com.ajeitai.backend.domain.prestador.Prestador;
import com.ajeitai.backend.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PrestadorService prestadorService;
    private final PortfolioRepository portfolioRepository;
    private final ArmazenamentoMidiaService armazenamentoMidiaService;

    public List<PortfolioItem> listarPorPrestador(Long prestadorId) {
        return portfolioRepository.findByPrestadorIdOrderByIdDesc(prestadorId);
    }

    @Transactional
    public PortfolioItem adicionar(String prestadorKeycloakId, String titulo, String descricao, MultipartFile arquivo) throws IOException {
        Prestador prestador = prestadorService.buscarPorKeycloakId(prestadorKeycloakId);
        String pasta = "prestador-" + prestador.getId() + "/portfolio";
        String caminho = armazenamentoMidiaService.salvar(pasta, arquivo);
        PortfolioItem item = PortfolioItem.builder()
                .prestador(prestador)
                .titulo(titulo)
                .descricao(descricao)
                .imagemUrl(caminho)
                .build();
        return portfolioRepository.save(item);
    }

    @Transactional
    public void remover(String prestadorKeycloakId, Long itemId) {
        Prestador prestador = prestadorService.buscarPorKeycloakId(prestadorKeycloakId);
        PortfolioItem item = portfolioRepository.findByIdAndPrestadorId(itemId, prestador.getId())
                .orElseThrow(() -> new IllegalArgumentException("Item de portfólio não encontrado."));
        portfolioRepository.delete(item);
    }
}
