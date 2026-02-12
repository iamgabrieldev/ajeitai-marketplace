package com.ajeitai.backend.repository;

import com.ajeitai.backend.domain.financeiro.TipoTransacaoWallet;
import com.ajeitai.backend.domain.financeiro.TransacaoWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface TransacaoWalletRepository extends JpaRepository<TransacaoWallet, Long> {

    boolean existsByPagamentoId(Long pagamentoId);

    @Query("SELECT COALESCE(SUM(t.valorBruto), 0) FROM TransacaoWallet t WHERE t.tipo = :tipo")
    BigDecimal sumValorBrutoByTipo(@Param("tipo") TipoTransacaoWallet tipo);

    @Query("SELECT COALESCE(SUM(t.taxaPlataforma), 0) FROM TransacaoWallet t WHERE t.tipo = :tipo")
    BigDecimal sumTaxaPlataformaByTipo(@Param("tipo") TipoTransacaoWallet tipo);
}

