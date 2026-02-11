package com.ajeitai.backend.domain.prestador;

import java.math.BigDecimal;

public record DashboardPrestador(
        Integer quantidadeTrabalhosMes,
        BigDecimal ganhoBrutoMes,
        BigDecimal lucroLiquidoMes
) {
}
