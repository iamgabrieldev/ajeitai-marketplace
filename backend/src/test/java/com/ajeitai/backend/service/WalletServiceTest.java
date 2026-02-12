package com.ajeitai.backend.service;

import com.ajeitai.backend.domain.agendamento.Agendamento;
import com.ajeitai.backend.domain.financeiro.TipoTransacaoWallet;
import com.ajeitai.backend.domain.financeiro.WalletPrestador;
import com.ajeitai.backend.domain.pagamento.Pagamento;
import com.ajeitai.backend.domain.prestador.Prestador;
import com.ajeitai.backend.repository.TransacaoWalletRepository;
import com.ajeitai.backend.repository.WalletPrestadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WalletServiceTest {

    private WalletPrestadorRepository walletPrestadorRepository;
    private TransacaoWalletRepository transacaoWalletRepository;
    private WalletService walletService;

    @BeforeEach
    void setup() {
        walletPrestadorRepository = mock(WalletPrestadorRepository.class);
        transacaoWalletRepository = mock(TransacaoWalletRepository.class);
        walletService = new WalletService(walletPrestadorRepository, transacaoWalletRepository);
    }

    @Test
    void creditarPorPagamentoConfirmado_quandoJaExisteTransacaoParaPagamento_naoCredita() {
        Prestador prestador = Prestador.builder().id(1L).build();
        Agendamento agendamento = Agendamento.builder()
                .id(10L)
                .prestador(prestador)
                .valorServico(new BigDecimal("100.00"))
                .build();
        Pagamento pagamento = Pagamento.builder().id(5L).build();

        when(transacaoWalletRepository.existsByPagamentoId(5L)).thenReturn(true);

        walletService.creditarPorPagamentoConfirmado(agendamento, pagamento);

        verify(transacaoWalletRepository).existsByPagamentoId(5L);
        verify(walletPrestadorRepository, never()).findByPrestadorId(any());
        verify(transacaoWalletRepository, never()).save(any());
    }

    @Test
    void creditarPorPagamentoConfirmado_creditaValorLiquidoComTaxa7PorCento() {
        Prestador prestador = Prestador.builder().id(1L).build();
        Agendamento agendamento = Agendamento.builder()
                .id(10L)
                .prestador(prestador)
                .valorServico(new BigDecimal("100.00"))
                .build();
        Pagamento pagamento = Pagamento.builder().id(5L).build();

        when(transacaoWalletRepository.existsByPagamentoId(5L)).thenReturn(false);
        when(walletPrestadorRepository.findByPrestadorId(1L)).thenReturn(Optional.empty());
        when(walletPrestadorRepository.save(any(WalletPrestador.class))).thenAnswer(i -> i.getArgument(0));
        when(transacaoWalletRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        walletService.creditarPorPagamentoConfirmado(agendamento, pagamento);

        verify(walletPrestadorRepository).findByPrestadorId(1L);
        verify(transacaoWalletRepository).save(argThat(t ->
                t.getTipo() == TipoTransacaoWallet.CREDITO_AGENDAMENTO
                        && t.getValorBruto().compareTo(new BigDecimal("100.00")) == 0
                        && t.getTaxaPlataforma().compareTo(new BigDecimal("7.00")) == 0
                        && t.getValorLiquido().compareTo(new BigDecimal("93.00")) == 0
        ));
        verify(walletPrestadorRepository).save(argThat(w ->
                w.getSaldoDisponivel().compareTo(new BigDecimal("93.00")) == 0
        ));
    }
}
