package com.ajeitai.backend.repository;

import com.ajeitai.backend.domain.prestador.DocumentoPrestador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentoPrestadorRepository extends JpaRepository<DocumentoPrestador, Long> {
    List<DocumentoPrestador> findByPrestadorIdOrderByIdDesc(Long prestadorId);

    Optional<DocumentoPrestador> findByIdAndPrestadorId(Long id, Long prestadorId);

    void deleteByIdAndPrestadorId(Long id, Long prestadorId);
}
