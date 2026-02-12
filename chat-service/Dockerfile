FROM golang:1.23-alpine AS builder
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -o /chat-service ./cmd/server

FROM alpine:3.19
RUN adduser -D app
WORKDIR /app
COPY --from=builder /chat-service .
COPY --from=builder /app/migrations ./migrations
USER app
EXPOSE 8080
CMD ["./chat-service"]
