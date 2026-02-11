package com.ajeitai.backend.repository;

import com.ajeitai.backend.domain.agendamento.Disponibilidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisponibilidadeRepository extends JpaRepository<Disponibilidade, Long> {
    List<Disponibilidade> findByPrestadorIdOrderByDiaSemanaAscHoraInicioAsc(Long prestadorId);

    List<Disponibilidade> findByPrestadorIdAndDiaSemanaOrderByHoraInicioAsc(Long prestadorId, Integer diaSemana);

    void deleteByPrestadorId(Long prestadorId);
}
