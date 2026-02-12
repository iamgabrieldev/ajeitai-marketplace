package com.ajeitai.backend.service;

import com.ajeitai.backend.domain.agendamento.Agendamento;
import com.ajeitai.backend.domain.agendamento.FormaPagamento;
import com.ajeitai.backend.domain.pagamento.Pagamento;
import com.ajeitai.backend.domain.pagamento.StatusPagamento;
import com.ajeitai.backend.integration.abacatepay.AbacatePayService;
import com.ajeitai.backend.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final AbacatePayService abacatePayService;

    @Transactional
    public Pagamento criarPagamento(Agendamento agendamento) {
        return pagamentoRepository.findByAgendamentoId(agendamento.getId())
                .orElseGet(() -> {
                    StatusPagamento status = definirStatusInicial(agendamento.getFormaPagamento());
                    String linkPagamento = null;
                    String billingId = null;

                    if (agendamento.getFormaPagamento() == FormaPagamento.ONLINE) {
                        AbacatePayService.BillingResult result = abacatePayService.createBilling(agendamento);
                        if (result != null) {
                            linkPagamento = result.paymentUrl();
                            billingId = result.billingId();
                        } else {
                            linkPagamento = "https://pagamentos.ajeitai.com/checkout/" + agendamento.getId();
                        }
                    }

                    Pagamento pagamento = Pagamento.builder()
                            .agendamento(agendamento)
                            .status(status)
                            .linkPagamento(linkPagamento)
                            .billingId(billingId)
                            .build();
                    return pagamentoRepository.save(pagamento);
                });
    }

    @Transactional
    public Pagamento confirmarPagamento(Long agendamentoId) {
        Pagamento pagamento = pagamentoRepository.findByAgendamentoId(agendamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Pagamento não encontrado."));
        if (pagamento.getStatus() != StatusPagamento.CONFIRMADO) {
            pagamento.confirmar();
        }
        return pagamentoRepository.save(pagamento);
    }


    public Pagamento buscarPorAgendamento(Long agendamentoId) {
        return pagamentoRepository.findByAgendamentoId(agendamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Pagamento não encontrado."));
    }

    @Transactional
    public void cancelarPorAgendamento(Long agendamentoId) {
        pagamentoRepository.findByAgendamentoId(agendamentoId).ifPresent(p -> {
            p.setStatus(StatusPagamento.CANCELADO);
            pagamentoRepository.save(p);
        });
    }

    private StatusPagamento definirStatusInicial(FormaPagamento formaPagamento) {
        if (formaPagamento == FormaPagamento.DINHEIRO) {
            return StatusPagamento.NAO_APLICAVEL;
        }
        return StatusPagamento.PENDENTE;
    }

}
