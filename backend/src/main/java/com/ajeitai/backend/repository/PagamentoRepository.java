package com.ajeitai.backend.repository;

import com.ajeitai.backend.domain.pagamento.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    Optional<Pagamento> findByAgendamentoId(Long agendamentoId);
}
