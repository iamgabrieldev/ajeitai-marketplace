package com.ajeitai.backend.service;

import com.ajeitai.backend.domain.agendamento.StatusAgendamento;
import com.ajeitai.backend.domain.catalogo.PrestadorPublicoDetalhe;
import com.ajeitai.backend.domain.catalogo.PrestadorPublicoResumo;
import com.ajeitai.backend.domain.catalogo.PortfolioPublicoItem;
import com.ajeitai.backend.domain.prestador.CategoriaAtuacao;
import com.ajeitai.backend.domain.prestador.Prestador;
import com.ajeitai.backend.repository.AgendamentoRepository;
import com.ajeitai.backend.repository.AvaliacaoRepository;
import com.ajeitai.backend.repository.PortfolioRepository;
import com.ajeitai.backend.repository.PrestadorRepository;
import com.ajeitai.backend.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final PrestadorRepository prestadorRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final PortfolioRepository portfolioRepository;

    @Cacheable(cacheNames = CacheConfig.CACHE_CATALOGO_LISTA, key = "#categoria + '-' + #cidade + '-' + #uf + '-' + #minAvaliacao + '-' + #latitude + '-' + #longitude")
    public List<PrestadorPublicoResumo> listarPrestadores(
            CategoriaAtuacao categoria,
            String cidade,
            String uf,
            Double minAvaliacao,
            Double latitude,
            Double longitude
    ) {
        List<Prestador> prestadores = prestadorRepository.findAll();
        return prestadores.stream()
                .filter(p -> p.getAtivo() == null || p.getAtivo())
                .filter(p -> categoria == null || p.getCategoria() == categoria)
                .filter(p -> cidade == null || (p.getEndereco() != null && cidade.equalsIgnoreCase(p.getEndereco().getCidade())))
                .filter(p -> uf == null || (p.getEndereco() != null && uf.equalsIgnoreCase(p.getEndereco().getUf())))
                .map(p -> toResumo(p, latitude, longitude))
                .filter(r -> minAvaliacao == null || r.mediaAvaliacao() >= minAvaliacao)
                .sorted(Comparator.comparing(PrestadorPublicoResumo::distanciaKm, Comparator.nullsLast(Double::compareTo)))
                .collect(Collectors.toList());
    }

    @Cacheable(cacheNames = CacheConfig.CACHE_CATALOGO_DETALHE, key = "#prestadorId")
    public PrestadorPublicoDetalhe buscarDetalhe(Long prestadorId) {
        Prestador prestador = prestadorRepository.findById(prestadorId)
                .orElseThrow(() -> new IllegalArgumentException("Prestador não encontrado."));
        Double media = avaliacaoRepository.obterMediaPorPrestador(prestadorId);
        List<PortfolioPublicoItem> portfolio = portfolioRepository.findByPrestadorIdOrderByIdDesc(prestadorId)
                .stream()
                .map(item -> new PortfolioPublicoItem(item.getId(), item.getTitulo(), item.getDescricao(), item.getImagemUrl()))
                .collect(Collectors.toList());
        return new PrestadorPublicoDetalhe(
                prestador.getId(),
                prestador.getNomeFantasia(),
                prestador.getCategoria(),
                prestador.getEndereco() != null ? prestador.getEndereco().getCidade() : null,
                prestador.getEndereco() != null ? prestador.getEndereco().getUf() : null,
                prestador.getValorServico(),
                media != null ? media : 0.0,
                prestador.getAvatarUrl(),
                portfolio,
                prestador.getKeycloakId()
        );
    }

    /**
     * Lista prestadores com paginação e filtros para clientes (GET /api/prestadores).
     */
    public Page<PrestadorPublicoResumo> listarPrestadoresPaginated(
            Pageable pageable,
            String search,
            CategoriaAtuacao categoria,
            Double minAvaliacao,
            String orderBy,
            Double latitude,
            Double longitude
    ) {
        List<Prestador> all = prestadorRepository.findAll();
        List<PrestadorPublicoResumo> list = all.stream()
                .filter(p -> p.getAtivo() == null || p.getAtivo())
                .filter(p -> categoria == null || p.getCategoria() == categoria)
                .filter(p -> search == null || search.isBlank()
                        || (p.getNomeFantasia() != null && p.getNomeFantasia().toLowerCase().contains(search.toLowerCase())))
                .map(p -> toResumo(p, latitude, longitude))
                .filter(r -> minAvaliacao == null || r.mediaAvaliacao() >= minAvaliacao)
                .sorted(comparatorForOrderBy(orderBy, latitude, longitude))
                .collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        List<PrestadorPublicoResumo> pageContent = list.subList(start, end);
        return new PageImpl<>(pageContent, pageable, list.size());
    }

    private PrestadorPublicoResumo toResumo(Prestador p, Double latitude, Double longitude) {
        Double media = avaliacaoRepository.obterMediaPorPrestador(p.getId());
        long totalAval = avaliacaoRepository.countByPrestadorId(p.getId());
        long totalServ = agendamentoRepository.countByPrestadorIdAndStatus(p.getId(), StatusAgendamento.REALIZADO);
        Double distancia = calcularDistanciaKm(latitude, longitude, p);
        return new PrestadorPublicoResumo(
                p.getId(),
                p.getNomeFantasia(),
                p.getCategoria(),
                p.getEndereco() != null ? p.getEndereco().getCidade() : null,
                p.getEndereco() != null ? p.getEndereco().getUf() : null,
                p.getValorServico(),
                media != null ? media : 0.0,
                totalAval,
                distancia,
                totalServ,
                p.getAvatarUrl()
        );
    }

    private Comparator<PrestadorPublicoResumo> comparatorForOrderBy(String orderBy, Double latitude, Double longitude) {
        if (orderBy == null || orderBy.isBlank()) {
            return Comparator.comparing(PrestadorPublicoResumo::distanciaKm, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        return switch (orderBy.toLowerCase()) {
            case "avaliacao" -> Comparator.comparing(PrestadorPublicoResumo::mediaAvaliacao, Comparator.reverseOrder());
            case "valorHora", "valorservico" -> Comparator.comparing(PrestadorPublicoResumo::valorServico,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "experiencia", "popularidade" -> Comparator.comparing(PrestadorPublicoResumo::totalServicos,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            default -> Comparator.comparing(PrestadorPublicoResumo::distanciaKm, Comparator.nullsLast(Comparator.naturalOrder()));
        };
    }

    private Double calcularDistanciaKm(Double latitude, Double longitude, Prestador prestador) {
        if (latitude == null || longitude == null || prestador.getEndereco() == null) {
            return null;
        }
        Double lat2 = prestador.getEndereco().getLatitude();
        Double lon2 = prestador.getEndereco().getLongitude();
        if (lat2 == null || lon2 == null) {
            return null;
        }
        return haversine(latitude, longitude, lat2, lon2);
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
