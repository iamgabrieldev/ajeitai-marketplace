package com.ajeitai.backend.repository;

import com.ajeitai.backend.domain.notificacao.NotificacaoToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificacaoTokenRepository extends JpaRepository<NotificacaoToken, Long> {
    Optional<NotificacaoToken> findByToken(String token);

    List<NotificacaoToken> findByPrestadorId(Long prestadorId);
}
