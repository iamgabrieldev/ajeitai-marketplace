package com.ajeitai.backend.service;

import com.ajeitai.backend.domain.agendamento.Agendamento;
import com.ajeitai.backend.domain.financeiro.TipoTransacaoWallet;
import com.ajeitai.backend.domain.financeiro.TransacaoWallet;
import com.ajeitai.backend.domain.financeiro.WalletPrestador;
import com.ajeitai.backend.domain.pagamento.Pagamento;
import com.ajeitai.backend.domain.prestador.Prestador;
import com.ajeitai.backend.repository.TransacaoWalletRepository;
import com.ajeitai.backend.repository.WalletPrestadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class WalletService {

    private static final BigDecimal COMISSAO_PERCENTUAL = new BigDecimal("0.07");

    private final WalletPrestadorRepository walletPrestadorRepository;
    private final TransacaoWalletRepository transacaoWalletRepository;

    @Value("${app.wallet.comissao-percentual:0.07}")
    private BigDecimal comissaoPercentual = COMISSAO_PERCENTUAL;

    /**
     * Credita o valor líquido (após 7% de comissão) na wallet do prestador quando um pagamento de agendamento é confirmado.
     * Idempotente: não credita novamente se já existir transação para o mesmo pagamento.
     */
    @Transactional
    public void creditarPorPagamentoConfirmado(Agendamento agendamento, Pagamento pagamento) {
        if (transacaoWalletRepository.existsByPagamentoId(pagamento.getId())) {
            return;
        }
        Prestador prestador = agendamento.getPrestador();
        BigDecimal valorBruto = agendamento.getValorServico() != null ? agendamento.getValorServico() : BigDecimal.ZERO;
        if (valorBruto.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal taxa = valorBruto.multiply(comissaoPercentual).setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorLiquido = valorBruto.subtract(taxa).setScale(2, RoundingMode.HALF_UP);

        WalletPrestador wallet = walletPrestadorRepository.findByPrestadorId(prestador.getId())
                .orElseGet(() -> {
                    WalletPrestador w = WalletPrestador.builder()
                            .prestador(prestador)
                            .saldoDisponivel(BigDecimal.ZERO)
                            .build();
                    return walletPrestadorRepository.save(w);
                });

        TransacaoWallet transacao = TransacaoWallet.builder()
                .prestador(prestador)
                .tipo(TipoTransacaoWallet.CREDITO_AGENDAMENTO)
                .valorBruto(valorBruto)
                .taxaPlataforma(taxa)
                .valorLiquido(valorLiquido)
                .agendamento(agendamento)
                .pagamento(pagamento)
                .build();
        transacaoWalletRepository.save(transacao);

        wallet.setSaldoDisponivel(wallet.getSaldoDisponivel().add(valorLiquido).setScale(2, RoundingMode.HALF_UP));
        walletPrestadorRepository.save(wallet);
    }
}
