package com.ajeitai.backend.repository;

import com.ajeitai.backend.domain.financeiro.SaquePrestador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaquePrestadorRepository extends JpaRepository<SaquePrestador, Long> {

    List<SaquePrestador> findByPrestadorIdOrderBySolicitadoEmDesc(Long prestadorId);
}

