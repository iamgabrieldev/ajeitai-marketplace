-- RLS para tabelas de agendamento e pagamento (PostgreSQL somente)
-- Esta migration deve ser aplicada em ambientes PostgreSQL (ex.: produção).

ALTER TABLE agendamentos ENABLE ROW LEVEL SECURITY;
ALTER TABLE pagamentos ENABLE ROW LEVEL SECURITY;

-- Política de leitura por dono do recurso (cliente ou prestador) ou admin
CREATE POLICY agendamentos_rls_by_owner ON agendamentos
FOR SELECT
USING (
    current_setting('app.current_role', true) = 'ADMIN'
    OR (
        current_setting('app.current_cliente_id', true) IS NOT NULL
        AND cliente_id = current_setting('app.current_cliente_id', true)::bigint
    )
    OR (
        current_setting('app.current_prestador_id', true) IS NOT NULL
        AND prestador_id = current_setting('app.current_prestador_id', true)::bigint
    )
);

CREATE POLICY pagamentos_rls_by_owner ON pagamentos
FOR SELECT
USING (
    current_setting('app.current_role', true) = 'ADMIN'
    OR EXISTS (
        SELECT 1 FROM agendamentos a
        WHERE a.id = pagamentos.agendamento_id
          AND (
            (current_setting('app.current_cliente_id', true) IS NOT NULL
             AND a.cliente_id = current_setting('app.current_cliente_id', true)::bigint)
            OR
            (current_setting('app.current_prestador_id', true) IS NOT NULL
             AND a.prestador_id = current_setting('app.current_prestador_id', true)::bigint)
          )
    )
);

