# Backend Ajeitai

API REST para o sistema Ajeitai: clientes e prestadores de serviço com agendamento, integrado ao Keycloak para autenticação e roles.

## Requisitos

- Java 21
- PostgreSQL (ou H2 para testes)
- Keycloak (realm `ajeitai`, porta 8180 por padrão)

## Configuração do Keycloak

A autenticação e a criação de contas são feitas no Keycloak. O backend apenas vincula o perfil (Cliente ou Prestador) ao usuário após o login.

### 1. Realm

- Crie ou use o realm **ajeitai**.
- Em **Realm settings**, confira o **Issuer** (ex.: `http://localhost:8180/realms/ajeitai`). O `application.properties` deve ter o mesmo valor em `spring.security.oauth2.resourceserver.jwt.issuer-uri`.

### 2. Roles

Crie duas **Realm roles**:

- **cliente** – usuários que agendam serviços.
- **prestador** – usuários que prestam serviços e gerenciam agenda/solicitações.

### 3. Usuários

- Crie cada usuário em **Users** e atribua **uma** das roles acima (em **Role mapping**).
- O backend converte as roles do token (`realm_access.roles`) para `ROLE_cliente` e `ROLE_prestador` e protege as rotas com `@PreAuthorize`.

### 4. Cliente OAuth (opcional)

Se o frontend for obter tokens via Keycloak (Authorization Code ou Resource Owner Password), configure um **Client** no realm (confidencial ou público, conforme o fluxo).

## Fluxo de uso

1. **Registro/login**: feito no Keycloak (tela de registro ou Admin API).
2. **Vínculo de perfil**: após o primeiro login, o front chama:
   - **Cliente**: `POST /api/clientes/vincular` (JWT com role `cliente`) com nome, telefone, CPF, endereço.
   - **Prestador**: `POST /api/prestadores/vincular` (JWT com role `prestador`) com nome fantasia, CNPJ, categoria, endereço, etc.
3. O backend cria o registro Cliente ou Prestador associado ao `sub` (keycloak_id) do JWT e retorna o perfil.

## API principal

- **Clientes** (`/api/clientes`, role `cliente`): `POST /vincular`, `GET /me`, `PUT /me`.
- **Prestadores** (`/api/prestadores`, role `prestador`): `POST /vincular`, `GET /me`, `PUT /me`, `GET /me/solicitacoes`, `GET /me/disponibilidade`, `PUT /me/disponibilidade`.
- **Agendamentos** (`/api/agendamentos`): `POST` (cliente cria), `GET` (cliente lista os seus), `PUT /{id}/aceitar` e `PUT /{id}/recusar` (prestador).

## Banco de dados

O schema é gerenciado pelo Flyway em `src/main/resources/db/migration/`. O Hibernate usa `ddl-auto=validate`. Para desenvolvimento local com PostgreSQL, use o `compose.yaml` (Postgres) e, se quiser, adicione o Keycloak ao compose (veja abaixo).

## Docker Compose (desenvolvimento)

O `compose.yaml` na raiz sobe o PostgreSQL. Para subir também o Keycloak em ambiente de desenvolvimento:

1. Adicione um serviço Keycloak ao `compose.yaml` (imagem `quay.io/keycloak/keycloak`, porta 8180, variáveis de ambiente para admin e realm).
2. Aponte `spring.security.oauth2.resourceserver.jwt.issuer-uri` para `http://localhost:8180/realms/ajeitai` (ou a URL do Keycloak no compose).
3. Execute `docker compose up -d` e configure o realm e as roles no Admin Console.

Exemplo mínimo de serviço Keycloak no compose:

```yaml
  keycloak:
    image: quay.io/keycloak/keycloak:latest
    command: start-dev
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
    ports:
      - "8180:8080"
```

Depois, crie o realm `ajeitai` e as roles `cliente` e `prestador` pelo painel em http://localhost:8180.
