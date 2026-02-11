package handler

import (
	"encoding/json"
	"net/http"
	"time"

	"github.com/ajeitai/chat-service/internal/auth"
	"github.com/ajeitai/chat-service/internal/model"
	"github.com/ajeitai/chat-service/internal/repository"
	"github.com/gin-gonic/gin"
	"github.com/gorilla/websocket"
)

type Handler struct {
	convRepo *repository.ConversaRepo
	msgRepo  *repository.MensagemRepo
	upgrader websocket.Upgrader
}

func New(convRepo *repository.ConversaRepo, msgRepo *repository.MensagemRepo) *Handler {
	return &Handler{
		convRepo: convRepo,
		msgRepo:  msgRepo,
		upgrader: websocket.Upgrader{
			CheckOrigin: func(r *http.Request) bool { return true },
		},
	}
}

func (h *Handler) ListarConversas(c *gin.Context) {
	userID, ok := auth.UserID(c.Request.Context())
	if !ok {
		c.JSON(http.StatusUnauthorized, gin.H{"erro": "não autorizado"})
		return
	}
	list, err := h.convRepo.ListarPorUsuario(c.Request.Context(), userID, 50)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"erro": err.Error()})
		return
	}
	c.JSON(http.StatusOK, list)
}

func (h *Handler) CriarOuBuscarConversa(c *gin.Context) {
	userID, ok := auth.UserID(c.Request.Context())
	if !ok {
		c.JSON(http.StatusUnauthorized, gin.H{"erro": "não autorizado"})
		return
	}
	var req model.CriarConversaRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"erro": "prestadorId obrigatório"})
		return
	}
	conv, created, err := h.convRepo.BuscarOuCriar(c.Request.Context(), userID, req.PrestadorID, req.AgendamentoID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"erro": err.Error()})
		return
	}
	status := http.StatusOK
	if created {
		status = http.StatusCreated
	}
	c.JSON(status, conv)
}

func (h *Handler) ListarMensagens(c *gin.Context) {
	userID, ok := auth.UserID(c.Request.Context())
	if !ok {
		c.JSON(http.StatusUnauthorized, gin.H{"erro": "não autorizado"})
		return
	}
	conversaID := c.Param("id")
	conv, err := h.convRepo.PorID(c.Request.Context(), conversaID)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"erro": "conversa não encontrada"})
		return
	}
	if conv.ClienteID != userID && conv.PrestadorID != userID {
		c.JSON(http.StatusForbidden, gin.H{"erro": "acesso negado"})
		return
	}
	list, err := h.msgRepo.ListarPorConversa(c.Request.Context(), conversaID, 100, nil)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"erro": err.Error()})
		return
	}
	c.JSON(http.StatusOK, list)
}

func (h *Handler) EnviarMensagem(c *gin.Context) {
	userID, ok := auth.UserID(c.Request.Context())
	if !ok {
		c.JSON(http.StatusUnauthorized, gin.H{"erro": "não autorizado"})
		return
	}
	conversaID := c.Param("id")
	conv, err := h.convRepo.PorID(c.Request.Context(), conversaID)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"erro": "conversa não encontrada"})
		return
	}
	if conv.ClienteID != userID && conv.PrestadorID != userID {
		c.JSON(http.StatusForbidden, gin.H{"erro": "acesso negado"})
		return
	}
	var req model.EnviarMensagemRequest
	if err := c.ShouldBindJSON(&req); err != nil || req.Texto == "" {
		c.JSON(http.StatusBadRequest, gin.H{"erro": "texto obrigatório"})
		return
	}
	msg := &model.Mensagem{
		ConversaID:  conversaID,
		RemetenteID: userID,
		Texto:       req.Texto,
	}
	if err := h.msgRepo.Inserir(c.Request.Context(), msg); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"erro": err.Error()})
		return
	}
	c.JSON(http.StatusCreated, msg)
}

func (h *Handler) WebSocket(c *gin.Context) {
	userID, ok := auth.UserID(c.Request.Context())
	if !ok {
		c.JSON(http.StatusUnauthorized, gin.H{"erro": "não autorizado"})
		return
	}
	conversaID := c.Param("id")
	conv, err := h.convRepo.PorID(c.Request.Context(), conversaID)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"erro": "conversa não encontrada"})
		return
	}
	if conv.ClienteID != userID && conv.PrestadorID != userID {
		c.JSON(http.StatusForbidden, gin.H{"erro": "acesso negado"})
		return
	}
	conn, err := h.upgrader.Upgrade(c.Writer, c.Request, nil)
	if err != nil {
		return
	}
	defer conn.Close()
	for {
		_, data, err := conn.ReadMessage()
		if err != nil {
			break
		}
		var payload struct {
			Texto string `json:"texto"`
		}
		if json.Unmarshal(data, &payload) != nil || payload.Texto == "" {
			continue
		}
		msg := &model.Mensagem{
			ConversaID:  conversaID,
			RemetenteID: userID,
			Texto:       payload.Texto,
			EnviadaEm:   time.Now(),
		}
		if err := h.msgRepo.Inserir(c.Request.Context(), msg); err != nil {
			continue
		}
		_ = conn.WriteJSON(msg)
	}
}
