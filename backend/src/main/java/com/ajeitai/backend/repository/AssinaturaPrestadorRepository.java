package com.ajeitai.backend.repository;

import com.ajeitai.backend.domain.financeiro.AssinaturaPrestador;
import com.ajeitai.backend.domain.financeiro.StatusAssinatura;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface AssinaturaPrestadorRepository extends JpaRepository<AssinaturaPrestador, Long> {

    Optional<AssinaturaPrestador> findFirstByPrestadorIdAndStatusOrderByDataFimDesc(Long prestadorId, StatusAssinatura status);

    Optional<AssinaturaPrestador> findTopByPrestadorIdOrderByDataFimDesc(Long prestadorId);

    @Query("SELECT COUNT(a) FROM AssinaturaPrestador a WHERE a.status = :status AND a.dataFim >= :hoje")
    long countByStatusAndDataFimGreaterThanEqual(@Param("status") StatusAssinatura status, @Param("hoje") LocalDate hoje);
}

