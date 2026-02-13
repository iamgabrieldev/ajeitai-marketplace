package com.ajeitai.backend.repository;

import com.ajeitai.backend.domain.prestador.Prestador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface PrestadorRepository extends JpaRepository<Prestador, Long> {

    Optional<Prestador> findByKeycloakId(String keycloakId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Prestador p WHERE p.id = :id")
    Optional<Prestador> findByIdForUpdate(@Param("id") Long id);
}
