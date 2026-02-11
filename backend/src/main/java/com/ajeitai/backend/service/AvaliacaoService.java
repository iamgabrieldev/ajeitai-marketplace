package com.ajeitai.backend.service;

import com.ajeitai.backend.domain.agendamento.Agendamento;
import com.ajeitai.backend.domain.agendamento.StatusAgendamento;
import com.ajeitai.backend.domain.avaliacao.Avaliacao;
import com.ajeitai.backend.domain.avaliacao.DadosAvaliacao;
import com.ajeitai.backend.domain.cliente.Cliente;
import com.ajeitai.backend.repository.AgendamentoRepository;
import com.ajeitai.backend.repository.AvaliacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final ClienteService clienteService;

    @Transactional
    public Avaliacao avaliar(String clienteKeycloakId, Long agendamentoId, DadosAvaliacao dados) {
        Cliente cliente = clienteService.buscarPorKeycloakId(clienteKeycloakId);
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado."));
        if (!agendamento.getCliente().getId().equals(cliente.getId())) {
            throw new IllegalArgumentException("Somente o cliente do agendamento pode avaliar.");
        }
        if (agendamento.getStatus() != StatusAgendamento.REALIZADO) {
            throw new IllegalArgumentException("Somente agendamentos realizados podem ser avaliados.");
        }
        if (avaliacaoRepository.findByAgendamentoId(agendamentoId).isPresent()) {
            throw new IllegalArgumentException("Este agendamento já foi avaliado.");
        }
        LocalDateTime referencia = agendamento.getCheckoutEm() != null
                ? agendamento.getCheckoutEm()
                : agendamento.getDataHora();
        if (referencia != null && referencia.plusDays(7).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("O prazo para avaliação deste agendamento expirou.");
        }
        Avaliacao avaliacao = Avaliacao.builder()
                .agendamento(agendamento)
                .cliente(cliente)
                .prestador(agendamento.getPrestador())
                .nota(dados.nota())
                .comentario(dados.comentario())
                .build();
        return avaliacaoRepository.save(avaliacao);
    }
}
