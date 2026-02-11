-- Ajustes para novas funcionalidades (agendamento, avaliação, pagamento, catálogo)

ALTER TABLE clientes ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(255);
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

ALTER TABLE prestadores ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(255);
ALTER TABLE prestadores ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE prestadores ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

ALTER TABLE agendamentos ADD COLUMN IF NOT EXISTS forma_pagamento VARCHAR(20) DEFAULT 'ONLINE' NOT NULL;
ALTER TABLE agendamentos ADD COLUMN IF NOT EXISTS valor_servico NUMERIC(10,2);
ALTER TABLE agendamentos ADD COLUMN IF NOT EXISTS criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL;
ALTER TABLE agendamentos ADD COLUMN IF NOT EXISTS confirmado_em TIMESTAMP;
ALTER TABLE agendamentos ADD COLUMN IF NOT EXISTS checkin_em TIMESTAMP;
ALTER TABLE agendamentos ADD COLUMN IF NOT EXISTS checkout_em TIMESTAMP;
ALTER TABLE agendamentos ADD COLUMN IF NOT EXISTS checkin_latitude DOUBLE PRECISION;
ALTER TABLE agendamentos ADD COLUMN IF NOT EXISTS checkin_longitude DOUBLE PRECISION;
ALTER TABLE agendamentos ADD COLUMN IF NOT EXISTS checkout_latitude DOUBLE PRECISION;
ALTER TABLE agendamentos ADD COLUMN IF NOT EXISTS checkout_longitude DOUBLE PRECISION;
ALTER TABLE agendamentos ADD COLUMN IF NOT EXISTS end_latitude DOUBLE PRECISION;
ALTER TABLE agendamentos ADD COLUMN IF NOT EXISTS end_longitude DOUBLE PRECISION;

CREATE TABLE IF NOT EXISTS pagamentos (
    id BIGSERIAL PRIMARY KEY,
    agendamento_id BIGINT NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    link_pagamento VARCHAR(255),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmado_em TIMESTAMP,
    CONSTRAINT fk_pagamento_agendamento FOREIGN KEY (agendamento_id) REFERENCES agendamentos(id)
);

CREATE TABLE IF NOT EXISTS avaliacoes (
    id BIGSERIAL PRIMARY KEY,
    agendamento_id BIGINT NOT NULL UNIQUE,
    cliente_id BIGINT NOT NULL,
    prestador_id BIGINT NOT NULL,
    nota INTEGER NOT NULL,
    comentario VARCHAR(1000),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_avaliacao_agendamento FOREIGN KEY (agendamento_id) REFERENCES agendamentos(id),
    CONSTRAINT fk_avaliacao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_avaliacao_prestador FOREIGN KEY (prestador_id) REFERENCES prestadores(id)
);

CREATE TABLE IF NOT EXISTS portfolio_itens (
    id BIGSERIAL PRIMARY KEY,
    prestador_id BIGINT NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    descricao VARCHAR(1000),
    imagem_url VARCHAR(255) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_portfolio_prestador FOREIGN KEY (prestador_id) REFERENCES prestadores(id)
);

CREATE TABLE IF NOT EXISTS notificacao_tokens (
    id BIGSERIAL PRIMARY KEY,
    prestador_id BIGINT NOT NULL,
    token VARCHAR(512) NOT NULL UNIQUE,
    plataforma VARCHAR(50),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_token_prestador FOREIGN KEY (prestador_id) REFERENCES prestadores(id)
);
