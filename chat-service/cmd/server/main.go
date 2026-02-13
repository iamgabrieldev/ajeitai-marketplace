package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"strings"
	"time"

	"github.com/ajeitai/chat-service/internal/auth"
	"github.com/ajeitai/chat-service/internal/handler"
	"github.com/ajeitai/chat-service/internal/repository"
	"github.com/gin-gonic/gin"
	"github.com/jackc/pgx/v5/pgxpool"
	redis "github.com/redis/go-redis/v9"
)

func main() {
	databaseURL := os.Getenv("DATABASE_URL")
	if databaseURL == "" {
		databaseURL = "postgres://localhost:5432/ajeitai_db?sslmode=disable"
	}
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	redisAddr := os.Getenv("REDIS_ADDR")
	if redisAddr == "" {
		redisAddr = "redis:6379"
	}
	rdb := redis.NewClient(&redis.Options{
		Addr: redisAddr,
	})
	if err := rdb.Ping(ctx).Err(); err != nil {
		log.Fatal("conectar ao Redis: ", err)
	}
	defer rdb.Close()

	pool, err := pgxpool.New(ctx, databaseURL)
	if err != nil {
		log.Fatal("conectar ao Postgres: ", err)
	}
	defer pool.Close()

	if err := runMigrations(ctx, pool); err != nil {
		log.Fatal("migração: ", err)
	}

	convRepo := repository.NewConversaRepo(pool)
	msgRepo := repository.NewMensagemRepo(pool)
	h := handler.New(convRepo, msgRepo, rdb)

	gin.SetMode(gin.ReleaseMode)
	r := gin.New()
	r.Use(corsMiddleware())
	r.Use(gin.Recovery())

	r.GET("/api/chat/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"ok": true, "service": "ajeitai-chat"})
	})

	api := r.Group("/api/chat")
	api.Use(auth.GinMiddleware())
	{
		api.GET("/conversas", h.ListarConversas)
		api.POST("/conversas", h.CriarOuBuscarConversa)
		api.GET("/conversas/:id/mensagens", h.ListarMensagens)
		api.POST("/conversas/:id/mensagens", h.EnviarMensagem)
		api.GET("/conversas/:id/ws", h.WebSocket)
	}

	r.NoRoute(func(c *gin.Context) {
		c.JSON(http.StatusNotFound, gin.H{"erro": "rota não encontrada", "path": c.Request.URL.Path})
	})

	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}
	srv := &http.Server{Addr: ":" + port, Handler: r}
	go func() {
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatal(err)
		}
	}()
	log.Println("chat-service listening on :" + port)

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, os.Interrupt)
	<-quit
	shutdown, _ := context.WithTimeout(context.Background(), 5*time.Second)
	_ = srv.Shutdown(shutdown)
	log.Println("chat-service stopped")
}

func runMigrations(ctx context.Context, pool *pgxpool.Pool) error {
	const name = "001_chat_schema.sql"
	for _, path := range []string{"migrations/" + name, "../../migrations/" + name} {
		sql, err := os.ReadFile(path)
		if err != nil {
			continue
		}
		_, err = pool.Exec(ctx, string(sql))
		return err
	}
	return os.ErrNotExist // migration file not found (run from chat-service dir or set CWD)
}

func corsMiddleware() gin.HandlerFunc {
	allowedOrigins := []string{"http://localhost:3000", "http://127.0.0.1:3000"}
	if v := os.Getenv("CORS_ORIGIN"); v != "" {
		allowedOrigins = append(allowedOrigins, strings.Split(v, ",")...)
	}
	return func(c *gin.Context) {
		origin := c.GetHeader("Origin")
		if origin != "" && (strings.Contains(origin, "localhost") || strings.Contains(origin, "127.0.0.1")) {
			c.Header("Access-Control-Allow-Origin", origin)
		} else {
			for _, o := range allowedOrigins {
				if o == "*" {
					c.Header("Access-Control-Allow-Origin", "*")
					break
				}
				if o == origin {
					c.Header("Access-Control-Allow-Origin", origin)
					break
				}
			}
		}
		c.Header("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS")
		c.Header("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, Authorization")
		c.Header("Access-Control-Max-Age", "86400")
		if c.Request.Method == http.MethodOptions {
			c.AbortWithStatus(http.StatusNoContent)
			return
		}
		c.Next()
	}
}
