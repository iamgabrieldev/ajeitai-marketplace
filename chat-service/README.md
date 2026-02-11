# Chat Service (Ajeitai)

Microserviço de chat entre cliente e prestador. API REST + WebSocket, MongoDB, autenticação via JWT (Keycloak).

## Endpoints

- `GET /api/chat/conversas` — Lista conversas do usuário
- `POST /api/chat/conversas` — Cria ou retorna conversa (body: `prestadorId`, `agendamentoId?`)
- `GET /api/chat/conversas/:id/mensagens` — Lista mensagens da conversa
- `POST /api/chat/conversas/:id/mensagens` — Envia mensagem (body: `texto`)
- `GET /api/chat/conversas/:id/ws` — WebSocket para tempo real

## Variáveis de ambiente

- `MONGO_URI` — URI do MongoDB (default: `mongodb://localhost:27017`)
- `PORT` — Porta HTTP (default: 8080)

## Build e execução

```bash
go mod tidy
go run ./cmd/server
```

## Docker

```bash
docker build -t chat-service .
docker run -p 8080:8080 -e MONGO_URI=mongodb://host.docker.internal:27017 chat-service
```

## Frontend

Configure `NEXT_PUBLIC_CHAT_API_URL` (ex.: `https://chat.ajeitai.example.com/api/chat`) para apontar para este serviço.
