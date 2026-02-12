-- Assinatura mensal do prestador, wallet e saques

CREATE TABLE IF NOT EXISTS assinaturas_prestador (
    id BIGSERIAL PRIMARY KEY,
    prestador_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE,
    ultimo_pagamento_em TIMESTAMP,
    billing_id VARCHAR(100),
    valor_atual NUMERIC(10,2) NOT NULL,
    CONSTRAINT fk_assinatura_prestador FOREIGN KEY (prestador_id) REFERENCES prestadores(id)
);

CREATE INDEX IF NOT EXISTS idx_assinatura_prestador ON assinaturas_prestador (prestador_id);
CREATE INDEX IF NOT EXISTS idx_assinatura_status ON assinaturas_prestador (status);

CREATE TABLE IF NOT EXISTS wallet_prestador (
    id BIGSERIAL PRIMARY KEY,
    prestador_id BIGINT NOT NULL UNIQUE,
    saldo_disponivel NUMERIC(12,2) NOT NULL DEFAULT 0,
    data_ultimo_saque DATE,
    CONSTRAINT fk_wallet_prestador FOREIGN KEY (prestador_id) REFERENCES prestadores(id)
);

CREATE INDEX IF NOT EXISTS idx_wallet_prestador ON wallet_prestador (prestador_id);

CREATE TABLE IF NOT EXISTS saques_prestador (
    id BIGSERIAL PRIMARY KEY,
    prestador_id BIGINT NOT NULL,
    valor_solicitado NUMERIC(12,2) NOT NULL,
    valor_liquido NUMERIC(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    referencia_externa VARCHAR(100),
    solicitado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    concluido_em TIMESTAMP,
    CONSTRAINT fk_saque_prestador FOREIGN KEY (prestador_id) REFERENCES prestadores(id)
);

CREATE INDEX IF NOT EXISTS idx_saque_prestador ON saques_prestador (prestador_id);
CREATE INDEX IF NOT EXISTS idx_saque_status ON saques_prestador (status);

CREATE TABLE IF NOT EXISTS wallet_transacoes (
    id BIGSERIAL PRIMARY KEY,
    prestador_id BIGINT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    valor_bruto NUMERIC(12,2) NOT NULL,
    taxa_plataforma NUMERIC(12,2),
    valor_liquido NUMERIC(12,2) NOT NULL,
    agendamento_id BIGINT,
    pagamento_id BIGINT,
    saque_id BIGINT,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_trans_prestador FOREIGN KEY (prestador_id) REFERENCES prestadores(id),
    CONSTRAINT fk_wallet_trans_agendamento FOREIGN KEY (agendamento_id) REFERENCES agendamentos(id),
    CONSTRAINT fk_wallet_trans_pagamento FOREIGN KEY (pagamento_id) REFERENCES pagamentos(id),
    CONSTRAINT fk_wallet_trans_saque FOREIGN KEY (saque_id) REFERENCES saques_prestador(id)
);

CREATE INDEX IF NOT EXISTS idx_wallet_trans_prestador ON wallet_transacoes (prestador_id);
CREATE INDEX IF NOT EXISTS idx_wallet_trans_tipo ON wallet_transacoes (tipo);
CREATE INDEX IF NOT EXISTS idx_wallet_trans_data ON wallet_transacoes (criado_em);

