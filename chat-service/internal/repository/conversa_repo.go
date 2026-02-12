package repository

import (
	"context"
	"errors"

	"github.com/ajeitai/chat-service/internal/model"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type ConversaRepo struct {
	pool *pgxpool.Pool
}

func NewConversaRepo(pool *pgxpool.Pool) *ConversaRepo {
	return &ConversaRepo{pool: pool}
}

func (r *ConversaRepo) Criar(ctx context.Context, c *model.Conversa) error {
	q := `INSERT INTO conversas (cliente_id, prestador_id, agendamento_id)
		  VALUES ($1, $2, $3)
		  RETURNING id::text, criada_em, atualizada_em`
	row := r.pool.QueryRow(ctx, q, c.ClienteID, c.PrestadorID, nullIfEmpty(c.AgendamentoID))
	return row.Scan(&c.ID, &c.CriadaEm, &c.AtualizadaEm)
}

func (r *ConversaRepo) BuscarOuCriar(ctx context.Context, clienteID, prestadorID, agendamentoID string) (*model.Conversa, bool, error) {
	q := `SELECT id::text, cliente_id, prestador_id, COALESCE(agendamento_id,''), criada_em, atualizada_em
		  FROM conversas WHERE cliente_id = $1 AND prestador_id = $2`
	var c model.Conversa
	err := r.pool.QueryRow(ctx, q, clienteID, prestadorID).Scan(
		&c.ID, &c.ClienteID, &c.PrestadorID, &c.AgendamentoID, &c.CriadaEm, &c.AtualizadaEm)
	if err == nil {
		return &c, false, nil
	}
	if !errors.Is(err, pgx.ErrNoRows) {
		return nil, false, err
	}
	c = model.Conversa{
		ClienteID:     clienteID,
		PrestadorID:   prestadorID,
		AgendamentoID: agendamentoID,
	}
	if err := r.Criar(ctx, &c); err != nil {
		return nil, false, err
	}
	return &c, true, nil
}

func (r *ConversaRepo) ListarPorUsuario(ctx context.Context, userID string, limite int) ([]*model.Conversa, error) {
	q := `SELECT id::text, cliente_id, prestador_id, COALESCE(agendamento_id,''), criada_em, atualizada_em
		  FROM conversas WHERE cliente_id = $1 OR prestador_id = $1
		  ORDER BY atualizada_em DESC LIMIT $2`
	rows, err := r.pool.Query(ctx, q, userID, limite)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var list []*model.Conversa
	for rows.Next() {
		var c model.Conversa
		if err := rows.Scan(&c.ID, &c.ClienteID, &c.PrestadorID, &c.AgendamentoID, &c.CriadaEm, &c.AtualizadaEm); err != nil {
			return nil, err
		}
		list = append(list, &c)
	}
	return list, rows.Err()
}

func (r *ConversaRepo) PorID(ctx context.Context, id string) (*model.Conversa, error) {
	q := `SELECT id::text, cliente_id, prestador_id, COALESCE(agendamento_id,''), criada_em, atualizada_em
		  FROM conversas WHERE id = $1::uuid`
	var c model.Conversa
	err := r.pool.QueryRow(ctx, q, id).Scan(&c.ID, &c.ClienteID, &c.PrestadorID, &c.AgendamentoID, &c.CriadaEm, &c.AtualizadaEm)
	if err != nil {
		return nil, err
	}
	return &c, nil
}

func nullIfEmpty(s string) *string {
	if s == "" {
		return nil
	}
	return &s
}
