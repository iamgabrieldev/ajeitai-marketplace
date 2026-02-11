package model

import "time"

type Conversa struct {
	ID            string    `bson:"_id,omitempty" json:"id"`
	ClienteID     string    `bson:"clienteId" json:"clienteId"`
	PrestadorID   string    `bson:"prestadorId" json:"prestadorId"`
	AgendamentoID string    `bson:"agendamentoId,omitempty" json:"agendamentoId,omitempty"`
	CriadaEm      time.Time `bson:"criadaEm" json:"criadaEm"`
	AtualizadaEm  time.Time `bson:"atualizadaEm" json:"atualizadaEm"`
}

type Mensagem struct {
	ID          string    `bson:"_id,omitempty" json:"id"`
	ConversaID  string    `bson:"conversaId" json:"conversaId"`
	RemetenteID string    `bson:"remetenteId" json:"remetenteId"`
	Texto       string    `bson:"texto" json:"texto"`
	EnviadaEm   time.Time `bson:"enviadaEm" json:"enviadaEm"`
	Lida        bool      `bson:"lida" json:"lida"`
}

type CriarConversaRequest struct {
	PrestadorID   string `json:"prestadorId" binding:"required"`
	AgendamentoID string `json:"agendamentoId"`
}

type EnviarMensagemRequest struct {
	Texto string `json:"texto" binding:"required"`
}
