package com.ajeitai.backend.repository;

import com.ajeitai.backend.domain.financeiro.WalletPrestador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletPrestadorRepository extends JpaRepository<WalletPrestador, Long> {

    Optional<WalletPrestador> findByPrestadorId(Long prestadorId);
}

