-- Tabela agendamentos (solicitação de serviço)
CREATE TABLE agendamentos (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL REFERENCES clientes (id),
    prestador_id BIGINT NOT NULL REFERENCES prestadores (id),
    data_hora TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    observacao VARCHAR(500),
    end_logradouro VARCHAR(255),
    end_bairro VARCHAR(255),
    end_cep VARCHAR(10),
    end_numero VARCHAR(20),
    end_complemento VARCHAR(255),
    end_cidade VARCHAR(255),
    end_uf VARCHAR(2)
);

CREATE INDEX idx_agendamento_cliente ON agendamentos (cliente_id);
CREATE INDEX idx_agendamento_prestador ON agendamentos (prestador_id);

-- Tabela disponibilidades (agenda semanal do prestador)
CREATE TABLE disponibilidades (
    id BIGSERIAL PRIMARY KEY,
    prestador_id BIGINT NOT NULL REFERENCES prestadores (id),
    dia_semana INTEGER NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fim TIME NOT NULL
);

CREATE INDEX idx_disponibilidade_prestador ON disponibilidades (prestador_id);
