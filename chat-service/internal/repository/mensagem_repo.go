package repository

import (
	"context"
	"time"

	"github.com/ajeitai/chat-service/internal/model"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type MensagemRepo struct {
	pool *pgxpool.Pool
}

func NewMensagemRepo(pool *pgxpool.Pool) *MensagemRepo {
	return &MensagemRepo{pool: pool}
}

func (r *MensagemRepo) Inserir(ctx context.Context, m *model.Mensagem) error {
	q := `INSERT INTO mensagens (conversa_id, remetente_id, texto)
		  VALUES ($1::uuid, $2, $3)
		  RETURNING id::text, enviada_em`
	return r.pool.QueryRow(ctx, q, m.ConversaID, m.RemetenteID, m.Texto).Scan(&m.ID, &m.EnviadaEm)
}

func (r *MensagemRepo) ListarPorConversa(ctx context.Context, conversaID string, limite int64, antes *time.Time) ([]*model.Mensagem, error) {
	var rows pgx.Rows
	var err error
	if antes != nil {
		q := `SELECT id::text, conversa_id::text, remetente_id, texto, enviada_em, lida
			  FROM mensagens WHERE conversa_id = $1::uuid AND enviada_em < $2
			  ORDER BY enviada_em DESC LIMIT $3`
		rows, err = r.pool.Query(ctx, q, conversaID, antes, limite)
	} else {
		q := `SELECT id::text, conversa_id::text, remetente_id, texto, enviada_em, lida
			  FROM mensagens WHERE conversa_id = $1::uuid
			  ORDER BY enviada_em DESC LIMIT $2`
		rows, err = r.pool.Query(ctx, q, conversaID, limite)
	}
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var list []*model.Mensagem
	for rows.Next() {
		var m model.Mensagem
		if err := rows.Scan(&m.ID, &m.ConversaID, &m.RemetenteID, &m.Texto, &m.EnviadaEm, &m.Lida); err != nil {
			return nil, err
		}
		list = append(list, &m)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}
	for i, j := 0, len(list)-1; i < j; i, j = i+1, j-1 {
		list[i], list[j] = list[j], list[i]
	}
	return list, nil
}
