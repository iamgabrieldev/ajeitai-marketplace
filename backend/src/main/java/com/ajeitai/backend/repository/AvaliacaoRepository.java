package com.ajeitai.backend.repository;

import com.ajeitai.backend.domain.avaliacao.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {
    Optional<Avaliacao> findByAgendamentoId(Long agendamentoId);

    @Query("SELECT AVG(a.nota) FROM Avaliacao a WHERE a.prestador.id = :prestadorId")
    Double obterMediaPorPrestador(@Param("prestadorId") Long prestadorId);

    @Query("SELECT COUNT(a) FROM Avaliacao a WHERE a.prestador.id = :prestadorId")
    long countByPrestadorId(@Param("prestadorId") Long prestadorId);
}
