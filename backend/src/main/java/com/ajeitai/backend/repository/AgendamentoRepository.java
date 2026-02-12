package com.ajeitai.backend.repository;

import com.ajeitai.backend.domain.agendamento.Agendamento;
import com.ajeitai.backend.domain.agendamento.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.prestador.id = :prestadorId AND a.status = :status")
    long countByPrestadorIdAndStatus(@Param("prestadorId") Long prestadorId, @Param("status") StatusAgendamento status);

    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.prestador.keycloakId = :keycloakId AND a.status = :status")
    long countByPrestadorKeycloakIdAndStatus(@Param("keycloakId") String keycloakId, @Param("status") StatusAgendamento status);

    @Query("SELECT a FROM Agendamento a JOIN FETCH a.cliente JOIN FETCH a.prestador WHERE a.cliente.id = :clienteId ORDER BY a.dataHora DESC")
    List<Agendamento> findByClienteIdOrderByDataHoraDesc(@Param("clienteId") Long clienteId);

    @Query("SELECT a FROM Agendamento a JOIN FETCH a.cliente JOIN FETCH a.prestador WHERE a.cliente.id = :clienteId AND a.status = :status ORDER BY a.dataHora DESC")
    List<Agendamento> findByClienteIdAndStatusOrderByDataHoraDesc(@Param("clienteId") Long clienteId, @Param("status") StatusAgendamento status);

    @Query("SELECT a FROM Agendamento a JOIN FETCH a.cliente JOIN FETCH a.prestador WHERE a.prestador.id = :prestadorId ORDER BY a.dataHora DESC")
    List<Agendamento> findByPrestadorIdOrderByDataHoraDesc(@Param("prestadorId") Long prestadorId);

    @Query("SELECT a FROM Agendamento a JOIN FETCH a.cliente JOIN FETCH a.prestador WHERE a.prestador.id = :prestadorId AND a.status = :status ORDER BY a.dataHora DESC")
    List<Agendamento> findByPrestadorIdAndStatusOrderByDataHoraDesc(@Param("prestadorId") Long prestadorId, @Param("status") StatusAgendamento status);

    @Query("SELECT a FROM Agendamento a JOIN FETCH a.cliente JOIN FETCH a.prestador WHERE a.prestador.keycloakId = :keycloakId ORDER BY a.dataHora DESC")
    List<Agendamento> findByPrestadorKeycloakIdOrderByDataHoraDesc(@Param("keycloakId") String keycloakId);

    @Query("SELECT a FROM Agendamento a JOIN FETCH a.cliente JOIN FETCH a.prestador WHERE a.prestador.keycloakId = :keycloakId AND a.status = :status ORDER BY a.dataHora DESC")
    List<Agendamento> findByPrestadorKeycloakIdAndStatusOrderByDataHoraDesc(@Param("keycloakId") String keycloakId, @Param("status") StatusAgendamento status);

    @Query("SELECT a FROM Agendamento a WHERE a.prestador.id = :prestadorId AND a.status = :status AND a.dataHora BETWEEN :inicio AND :fim")
    List<Agendamento> findByPrestadorIdAndStatusAndDataHoraBetween(
            @Param("prestadorId") Long prestadorId,
            @Param("status") StatusAgendamento status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    @Query("SELECT a FROM Agendamento a WHERE a.prestador.keycloakId = :keycloakId AND a.status = :status AND a.dataHora BETWEEN :inicio AND :fim")
    List<Agendamento> findByPrestadorKeycloakIdAndStatusAndDataHoraBetween(
            @Param("keycloakId") String keycloakId,
            @Param("status") StatusAgendamento status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Agendamento a WHERE a.prestador.id = :prestadorId AND a.dataHora BETWEEN :inicio AND :fim AND a.status IN :statuses")
    boolean existsByPrestadorIdAndDataHoraBetweenAndStatusIn(
            @Param("prestadorId") Long prestadorId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("statuses") List<StatusAgendamento> statuses
    );

    List<Agendamento> findByStatusAndDataHoraBefore(StatusAgendamento status, LocalDateTime dataHoraLimit);

    long countByStatus(StatusAgendamento status);

    List<Agendamento> findByStatusOrderByDataHoraDesc(StatusAgendamento status);
}
