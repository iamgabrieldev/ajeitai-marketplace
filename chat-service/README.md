# Chat Service (Ajeitai)

Microserviço de chat entre cliente e prestador. API REST + WebSocket, PostgreSQL, autenticação via JWT (Keycloak).

## Endpoints

- `GET /api/chat/conversas` — Lista conversas do usuário
- `POST /api/chat/conversas` — Cria ou retorna conversa (body: `prestadorId`, `agendamentoId?`)
- `GET /api/chat/conversas/:id/mensagens` — Lista mensagens da conversa
- `POST /api/chat/conversas/:id/mensagens` — Envia mensagem (body: `texto`)
- `GET /api/chat/conversas/:id/ws` — WebSocket para tempo real

## Variáveis de ambiente

- `DATABASE_URL` — URL do PostgreSQL (ex.: `postgres://user:pass@host:5432/ajeitai_db?sslmode=disable`). Em desenvolvimento: default `postgres://localhost:5432/ajeitai_db?sslmode=disable`.
- `PORT` — Porta HTTP (default: 8080)

A migração do schema (tabelas `conversas` e `mensagens`) é executada automaticamente na subida do serviço.

## Requisitos

- **Go 1.23** ([golang.org/dl](https://go.dev/dl/)).

## Build e execução

Execute a partir do diretório `chat-service` para que o arquivo de migração seja encontrado:

```bash
go mod tidy
go run ./cmd/server
```

Ou, a partir de `cmd/server`: `go run .` (o código tenta também `../../migrations/`).

## Docker

```bash
docker build -t chat-service .
docker run -p 8080:8080 -e DATABASE_URL=postgres://user:pass@host:5432/ajeitai_db?sslmode=disable chat-service
```

## Frontend

Configure `NEXT_PUBLIC_CHAT_API_URL` (ex.: `https://chat.ajeitai.example.com/api/chat`) para apontar para este serviço.
