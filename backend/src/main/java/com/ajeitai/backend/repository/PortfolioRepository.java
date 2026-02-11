package com.ajeitai.backend.repository;

import com.ajeitai.backend.domain.portfolio.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<PortfolioItem, Long> {
    List<PortfolioItem> findByPrestadorIdOrderByIdDesc(Long prestadorId);

    Optional<PortfolioItem> findByIdAndPrestadorId(Long id, Long prestadorId);
}
