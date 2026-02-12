package com.ajeitai.backend.service;

import com.ajeitai.backend.domain.financeiro.SaquePrestador;
import com.ajeitai.backend.domain.financeiro.StatusSaque;
import com.ajeitai.backend.domain.financeiro.TipoTransacaoWallet;
import com.ajeitai.backend.domain.financeiro.TransacaoWallet;
import com.ajeitai.backend.domain.financeiro.WalletPrestador;
import com.ajeitai.backend.domain.prestador.Prestador;
import com.ajeitai.backend.repository.SaquePrestadorRepository;
import com.ajeitai.backend.repository.TransacaoWalletRepository;
import com.ajeitai.backend.repository.WalletPrestadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaqueService {

    private static final int DIAS_ENTRE_SAQUES = 10;

    private final PrestadorService prestadorService;
    private final WalletPrestadorRepository walletPrestadorRepository;
    private final SaquePrestadorRepository saquePrestadorRepository;
    private final TransacaoWalletRepository transacaoWalletRepository;

    @Value("${app.saque.dias-entre-saques:10}")
    private int diasEntreSaques = DIAS_ENTRE_SAQUES;

    /**
     * Resumo da wallet do prestador para o dashboard: saldo, último saque e quando pode sacar novamente.
     */
    public WalletResumo obterResumo(String keycloakId) {
        Prestador prestador = prestadorService.buscarPorKeycloakId(keycloakId);
        WalletPrestador wallet = walletPrestadorRepository.findByPrestadorId(prestador.getId())
                .orElseGet(() -> WalletPrestador.builder()
                        .prestador(prestador)
                        .saldoDisponivel(BigDecimal.ZERO)
                        .build());
        LocalDate ultimoSaque = wallet.getDataUltimoSaque();
        LocalDate proximoSaqueDisponivel = ultimoSaque == null
                ? LocalDate.now()
                : ultimoSaque.plusDays(diasEntreSaques);
        boolean podeSolicitar = wallet.getSaldoDisponivel().compareTo(BigDecimal.ZERO) > 0
                && !LocalDate.now().isBefore(proximoSaqueDisponivel);
        return new WalletResumo(
                wallet.getSaldoDisponivel(),
                ultimoSaque,
                proximoSaqueDisponivel,
                podeSolicitar
        );
    }

    /**
     * Solicita saque do saldo disponível. Regra: apenas a cada 10 dias e com saldo > 0.
     * O valor sacado é todo o saldo disponível.
     */
    @Transactional
    public SaquePrestador solicitarSaque(String keycloakId) {
        Prestador prestador = prestadorService.buscarPorKeycloakId(keycloakId);
        WalletPrestador wallet = walletPrestadorRepository.findByPrestadorId(prestador.getId())
                .orElseThrow(() -> new IllegalArgumentException("Você não possui saldo disponível para saque."));
        if (wallet.getSaldoDisponivel().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Saldo insuficiente para saque.");
        }
        LocalDate hoje = LocalDate.now();
        LocalDate ultimo = wallet.getDataUltimoSaque();
        if (ultimo != null && ChronoUnit.DAYS.between(ultimo, hoje) < diasEntreSaques) {
            throw new IllegalArgumentException(
                    "Saque permitido apenas a cada " + diasEntreSaques + " dias. Próximo saque disponível em: "
                            + ultimo.plusDays(diasEntreSaques) + ".");
        }
        BigDecimal valor = wallet.getSaldoDisponivel();
        SaquePrestador saque = SaquePrestador.builder()
                .prestador(prestador)
                .valorSolicitado(valor)
                .valorLiquido(valor)
                .status(StatusSaque.PENDENTE)
                .build();
        saque = saquePrestadorRepository.save(saque);

        wallet.setSaldoDisponivel(BigDecimal.ZERO);
        wallet.setDataUltimoSaque(hoje);
        walletPrestadorRepository.save(wallet);

        TransacaoWallet transacao = TransacaoWallet.builder()
                .prestador(prestador)
                .tipo(TipoTransacaoWallet.DEBITO_SAQUE)
                .valorBruto(valor)
                .taxaPlataforma(BigDecimal.ZERO)
                .valorLiquido(valor)
                .saque(saque)
                .build();
        transacaoWalletRepository.save(transacao);

        return saque;
    }

    public List<SaquePrestador> listarSaques(String keycloakId) {
        Prestador prestador = prestadorService.buscarPorKeycloakId(keycloakId);
        return saquePrestadorRepository.findByPrestadorIdOrderBySolicitadoEmDesc(prestador.getId());
    }

    public record WalletResumo(
            BigDecimal saldoDisponivel,
            LocalDate dataUltimoSaque,
            LocalDate proximoSaqueDisponivelEm,
            boolean podeSolicitarSaque
    ) {}
}
