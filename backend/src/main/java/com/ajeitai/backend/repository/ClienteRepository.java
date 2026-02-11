package com.ajeitai.backend.repository;

import com.ajeitai.backend.domain.cliente.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByKeycloakId(String keycloakId);
}
