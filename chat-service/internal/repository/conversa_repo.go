package repository

import (
	"context"
	"time"

	"github.com/ajeitai/chat-service/internal/model"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

type ConversaRepo struct {
	coll *mongo.Collection
}

func NewConversaRepo(db *mongo.Database) *ConversaRepo {
	return &ConversaRepo{coll: db.Collection("conversas")}
}

func (r *ConversaRepo) Criar(ctx context.Context, c *model.Conversa) error {
	now := time.Now()
	c.CriadaEm = now
	c.AtualizadaEm = now
	res, err := r.coll.InsertOne(ctx, c)
	if err != nil {
		return err
	}
	if oid, ok := res.InsertedID.(primitive.ObjectID); ok {
		c.ID = oid.Hex()
	}
	return nil
}

func (r *ConversaRepo) BuscarOuCriar(ctx context.Context, clienteID, prestadorID, agendamentoID string) (*model.Conversa, bool, error) {
	filter := bson.M{
		"clienteId":   clienteID,
		"prestadorId": prestadorID,
	}
	var c model.Conversa
	err := r.coll.FindOne(ctx, filter).Decode(&c)
	if err == nil {
		return &c, false, nil
	}
	if err != mongo.ErrNoDocuments {
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
	filter := bson.M{
		"$or": []bson.M{
			{"clienteId": userID},
			{"prestadorId": userID},
		},
	}
	opts := options.Find().SetSort(bson.M{"atualizadaEm": -1}).SetLimit(int64(limite))
	cur, err := r.coll.Find(ctx, filter, opts)
	if err != nil {
		return nil, err
	}
	defer cur.Close(ctx)
	var list []*model.Conversa
	if err := cur.All(ctx, &list); err != nil {
		return nil, err
	}
	return list, nil
}

func (r *ConversaRepo) PorID(ctx context.Context, id string) (*model.Conversa, error) {
	oid, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		return nil, err
	}
	var c model.Conversa
	err = r.coll.FindOne(ctx, bson.M{"_id": oid}).Decode(&c)
	if err != nil {
		return nil, err
	}
	return &c, nil
}
