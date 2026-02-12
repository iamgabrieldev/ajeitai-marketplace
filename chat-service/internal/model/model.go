package model

import "time"

type Conversa struct {
	ID            string    `json:"id" db:"id"`
	ClienteID     string    `json:"clienteId" db:"cliente_id"`
	PrestadorID   string    `json:"prestadorId" db:"prestador_id"`
	AgendamentoID string    `json:"agendamentoId,omitempty" db:"agendamento_id"`
	CriadaEm      time.Time `json:"criadaEm" db:"criada_em"`
	AtualizadaEm  time.Time `json:"atualizadaEm" db:"atualizada_em"`
}

type Mensagem struct {
	ID          string    `json:"id" db:"id"`
	ConversaID  string    `json:"conversaId" db:"conversa_id"`
	RemetenteID string    `json:"remetenteId" db:"remetente_id"`
	Texto       string    `json:"texto" db:"texto"`
	EnviadaEm   time.Time `json:"enviadaEm" db:"enviada_em"`
	Lida        bool      `json:"lida" db:"lida"`
}

type CriarConversaRequest struct {
	PrestadorID   string `json:"prestadorId" binding:"required"`
	AgendamentoID string `json:"agendamentoId"`
}

type EnviarMensagemRequest struct {
	Texto string `json:"texto" binding:"required"`
}
