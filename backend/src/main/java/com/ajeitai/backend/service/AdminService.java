package com.ajeitai.backend.service;

import com.ajeitai.backend.domain.agendamento.Agendamento;
import com.ajeitai.backend.domain.agendamento.StatusAgendamento;
import com.ajeitai.backend.domain.financeiro.AssinaturaPrestador;
import com.ajeitai.backend.domain.financeiro.SaquePrestador;
import com.ajeitai.backend.domain.financeiro.StatusAssinatura;
import com.ajeitai.backend.domain.financeiro.TipoTransacaoWallet;
import com.ajeitai.backend.domain.pagamento.Pagamento;
import com.ajeitai.backend.domain.pagamento.StatusPagamento;
import com.ajeitai.backend.domain.prestador.Prestador;
import com.ajeitai.backend.repository.AgendamentoRepository;
import com.ajeitai.backend.repository.AssinaturaPrestadorRepository;
import com.ajeitai.backend.repository.ClienteRepository;
import com.ajeitai.backend.repository.PagamentoRepository;
import com.ajeitai.backend.repository.PrestadorRepository;
import com.ajeitai.backend.repository.SaquePrestadorRepository;
import com.ajeitai.backend.repository.TransacaoWalletRepository;
import com.ajeitai.backend.repository.WalletPrestadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ClienteRepository clienteRepository;
    private final PrestadorRepository prestadorRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final AssinaturaPrestadorRepository assinaturaPrestadorRepository;
    private final PagamentoRepository pagamentoRepository;
    private final SaquePrestadorRepository saquePrestadorRepository;
    private final TransacaoWalletRepository transacaoWalletRepository;
    private final WalletPrestadorRepository walletPrestadorRepository;

    /**
     * Visão geral: contagens e métricas financeiras para o painel admin.
     */
    public VisaoGeralDto visaoGeral() {
        long totalClientes = clienteRepository.count();
        long totalPrestadores = prestadorRepository.count();
        LocalDate hoje = LocalDate.now();
        long prestadoresComAssinaturaAtiva = assinaturaPrestadorRepository
                .countByStatusAndDataFimGreaterThanEqual(StatusAssinatura.ATIVA, hoje);

        Map<String, Long> agendamentosPorStatus = new HashMap<>();
        for (StatusAgendamento s : StatusAgendamento.values()) {
            agendamentosPorStatus.put(s.name(), agendamentoRepository.countByStatus(s));
        }

        BigDecimal gmv = transacaoWalletRepository.sumValorBrutoByTipo(TipoTransacaoWallet.CREDITO_AGENDAMENTO);
        if (gmv == null) gmv = BigDecimal.ZERO;
        BigDecimal receitaComissao = transacaoWalletRepository.sumTaxaPlataformaByTipo(TipoTransacaoWallet.CREDITO_AGENDAMENTO);
        if (receitaComissao == null) receitaComissao = BigDecimal.ZERO;
        // Receita de assinaturas pode ser aproximada (prestadores ativos * valor mensal) ou de uma tabela de receita; por ora só comissão
        BigDecimal receitaPlataforma = receitaComissao;

        long pagamentosPendentes = pagamentoRepository.countByStatus(StatusPagamento.PENDENTE);
        long pagamentosConfirmados = pagamentoRepository.countByStatus(StatusPagamento.CONFIRMADO);

        return new VisaoGeralDto(
                totalClientes,
                totalPrestadores,
                prestadoresComAssinaturaAtiva,
                agendamentosPorStatus,
                gmv,
                receitaPlataforma,
                pagamentosPendentes,
                pagamentosConfirmados
        );
    }

    /**
     * Lista prestadores com dados de assinatura e saldo (para suporte).
     */
    public List<PrestadorAdminDto> listarPrestadores() {
        List<Prestador> prestadores = prestadorRepository.findAll();
        return prestadores.stream().map(p -> {
            String statusAssinatura = null;
            LocalDate dataFimAssinatura = null;
            AssinaturaPrestador ass = assinaturaPrestadorRepository.findTopByPrestadorIdOrderByDataFimDesc(p.getId()).orElse(null);
            if (ass != null) {
                statusAssinatura = ass.getStatus().name();
                dataFimAssinatura = ass.getDataFim();
            }
            BigDecimal saldo = walletPrestadorRepository.findByPrestadorId(p.getId())
                    .map(w -> w.getSaldoDisponivel())
                    .orElse(BigDecimal.ZERO);
            return new PrestadorAdminDto(
                    p.getId(),
                    p.getNomeFantasia(),
                    p.getEmail(),
                    statusAssinatura,
                    dataFimAssinatura,
                    saldo
            );
        }).collect(Collectors.toList());
    }

    /**
     * Lista agendamentos com filtro opcional por status.
     */
    public List<Agendamento> listarAgendamentos(StatusAgendamento status) {
        if (status != null) {
            return agendamentoRepository.findByStatusOrderByDataHoraDesc(status).stream().limit(500).collect(Collectors.toList());
        }
        return agendamentoRepository.findAll().stream()
                .sorted((a, b) -> b.getDataHora().compareTo(a.getDataHora()))
                .limit(500)
                .collect(Collectors.toList());
    }

    /**
     * Lista pagamentos (últimos, para conciliação).
     */
    @Transactional(readOnly = true)
    public List<PagamentoAdminDto> listarPagamentos() {
        return pagamentoRepository.findAll().stream()
                .sorted((a, b) -> (b.getCriadoEm() != null && a.getCriadoEm() != null) ? b.getCriadoEm().compareTo(a.getCriadoEm()) : 0)
                .limit(200)
                .map(p -> new PagamentoAdminDto(
                        p.getId(),
                        p.getAgendamento() != null ? p.getAgendamento().getId() : null,
                        p.getStatus().name(),
                        p.getBillingId(),
                        p.getCriadoEm(),
                        p.getConfirmadoEm()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Lista saques de prestadores.
     */
    @Transactional(readOnly = true)
    public List<SaqueAdminDto> listarSaques() {
        return saquePrestadorRepository.findAll().stream()
                .sorted((a, b) -> b.getSolicitadoEm().compareTo(a.getSolicitadoEm()))
                .limit(200)
                .map(s -> new SaqueAdminDto(
                        s.getId(),
                        s.getPrestador() != null ? s.getPrestador().getId() : null,
                        s.getPrestador() != null ? s.getPrestador().getNomeFantasia() : null,
                        s.getValorSolicitado(),
                        s.getValorLiquido(),
                        s.getStatus().name(),
                        s.getSolicitadoEm(),
                        s.getConcluidoEm()
                ))
                .collect(Collectors.toList());
    }

    public record VisaoGeralDto(
            long totalClientes,
            long totalPrestadores,
            long prestadoresComAssinaturaAtiva,
            Map<String, Long> agendamentosPorStatus,
            BigDecimal gmv,
            BigDecimal receitaPlataforma,
            long pagamentosPendentes,
            long pagamentosConfirmados
    ) {}

    public record PrestadorAdminDto(
            Long id,
            String nomeFantasia,
            String email,
            String statusAssinatura,
            LocalDate dataFimAssinatura,
            BigDecimal saldoDisponivel
    ) {}

    public record PagamentoAdminDto(
            Long id,
            Long agendamentoId,
            String status,
            String billingId,
            LocalDateTime criadoEm,
            LocalDateTime confirmadoEm
    ) {}

    public record SaqueAdminDto(
            Long id,
            Long prestadorId,
            String prestadorNome,
            BigDecimal valorSolicitado,
            BigDecimal valorLiquido,
            String status,
            LocalDateTime solicitadoEm,
            LocalDateTime concluidoEm
    ) {}
}
