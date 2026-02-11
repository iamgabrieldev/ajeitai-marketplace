package com.ajeitai.backend.repository;

import com.ajeitai.backend.domain.prestador.Prestador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrestadorRepository extends JpaRepository<Prestador, Long> {
    Optional<Prestador> findByKeycloakId(String keycloakId);
}
