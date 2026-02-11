package com.ajeitai.backend.service;

import com.ajeitai.backend.domain.notificacao.DadosTokenPush;
import com.ajeitai.backend.domain.notificacao.NotificacaoToken;
import com.ajeitai.backend.domain.prestador.Prestador;
import com.ajeitai.backend.repository.NotificacaoTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final PrestadorService prestadorService;
    private final NotificacaoTokenRepository notificacaoTokenRepository;

    @Transactional
    public NotificacaoToken registrarToken(String prestadorKeycloakId, DadosTokenPush dados) {
        Prestador prestador = prestadorService.buscarPorKeycloakId(prestadorKeycloakId);
        return notificacaoTokenRepository.findByToken(dados.token())
                .map(token -> {
                    token.setPrestador(prestador);
                    token.setPlataforma(dados.plataforma());
                    return notificacaoTokenRepository.save(token);
                })
                .orElseGet(() -> {
                    NotificacaoToken token = NotificacaoToken.builder()
                            .prestador(prestador)
                            .token(dados.token())
                            .plataforma(dados.plataforma())
                            .build();
                    return notificacaoTokenRepository.save(token);
                });
    }
}
