-- Tabela de documentos/portfólio do prestador
CREATE TABLE documentos_prestador (
    id BIGSERIAL PRIMARY KEY,
    prestador_id BIGINT NOT NULL REFERENCES prestadores (id) ON DELETE CASCADE,
    nome_arquivo VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    caminho VARCHAR(500) NOT NULL,
    descricao VARCHAR(500)
);

CREATE INDEX idx_documento_prestador_id ON documentos_prestador (prestador_id);
