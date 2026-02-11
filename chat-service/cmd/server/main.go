package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"time"

	"github.com/ajeitai/chat-service/internal/auth"
	"github.com/ajeitai/chat-service/internal/handler"
	"github.com/ajeitai/chat-service/internal/repository"
	"github.com/gin-gonic/gin"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

func main() {
	mongoURI := os.Getenv("MONGO_URI")
	if mongoURI == "" {
		mongoURI = "mongodb://localhost:27017"
	}
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	client, err := mongo.Connect(ctx, options.Client().ApplyURI(mongoURI))
	if err != nil {
		log.Fatal(err)
	}
	defer client.Disconnect(context.Background())

	db := client.Database("ajeitai_chat")
	convRepo := repository.NewConversaRepo(db)
	msgRepo := repository.NewMensagemRepo(db)
	h := handler.New(convRepo, msgRepo)

	gin.SetMode(gin.ReleaseMode)
	r := gin.New()
	r.Use(gin.Recovery())

	api := r.Group("/api/chat")
	api.Use(auth.GinMiddleware())
	{
		api.GET("/conversas", h.ListarConversas)
		api.POST("/conversas", h.CriarOuBuscarConversa)
		api.GET("/conversas/:id/mensagens", h.ListarMensagens)
		api.POST("/conversas/:id/mensagens", h.EnviarMensagem)
		api.GET("/conversas/:id/ws", h.WebSocket)
	}

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
