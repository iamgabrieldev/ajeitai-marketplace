-- Tabela clientes (endereço embutido)
CREATE TABLE clientes (
    id BIGSERIAL PRIMARY KEY,
    keycloak_id VARCHAR(255) NOT NULL,
    cpf VARCHAR(14),
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    telefone VARCHAR(20),
    ativo BOOLEAN,
    logradouro VARCHAR(255),
    bairro VARCHAR(255),
    cep VARCHAR(10),
    numero VARCHAR(20),
    complemento VARCHAR(255),
    cidade VARCHAR(255),
    uf VARCHAR(2)
);

CREATE UNIQUE INDEX idx_cliente_keycloak ON clientes (keycloak_id);

-- Tabela prestadores (endereço embutido)
CREATE TABLE prestadores (
    id BIGSERIAL PRIMARY KEY,
    keycloak_id VARCHAR(255) NOT NULL,
    nome_fantasia VARCHAR(255),
    cnpj VARCHAR(18),
    categoria VARCHAR(100),
    email VARCHAR(255),
    telefone VARCHAR(20),
    ativo BOOLEAN,
    logradouro VARCHAR(255),
    bairro VARCHAR(255),
    cep VARCHAR(10),
    numero VARCHAR(20),
    complemento VARCHAR(255),
    cidade VARCHAR(255),
    uf VARCHAR(2)
);

CREATE UNIQUE INDEX idx_prestador_keycloak ON prestadores (keycloak_id);
