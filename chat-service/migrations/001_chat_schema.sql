-- Schema do chat (idempotente)
-- Conversas: uma por par (cliente, prestador)
CREATE TABLE IF NOT EXISTS conversas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id VARCHAR(36) NOT NULL,
    prestador_id VARCHAR(36) NOT NULL,
    agendamento_id VARCHAR(36),
    criada_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizada_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(cliente_id, prestador_id)
);

CREATE INDEX IF NOT EXISTS idx_conversas_cliente_id ON conversas (cliente_id);
CREATE INDEX IF NOT EXISTS idx_conversas_prestador_id ON conversas (prestador_id);
CREATE INDEX IF NOT EXISTS idx_conversas_atualizada_em ON conversas (atualizada_em DESC);

-- Mensagens de cada conversa
CREATE TABLE IF NOT EXISTS mensagens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversa_id UUID NOT NULL REFERENCES conversas(id) ON DELETE CASCADE,
    remetente_id VARCHAR(36) NOT NULL,
    texto TEXT NOT NULL,
    enviada_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    lida BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_mensagens_conversa_enviada ON mensagens (conversa_id, enviada_em DESC);
