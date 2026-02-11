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

type MensagemRepo struct {
	coll *mongo.Collection
}

func NewMensagemRepo(db *mongo.Database) *MensagemRepo {
	return &MensagemRepo{coll: db.Collection("mensagens")}
}

func (r *MensagemRepo) Inserir(ctx context.Context, m *model.Mensagem) error {
	m.EnviadaEm = time.Now()
	res, err := r.coll.InsertOne(ctx, m)
	if err != nil {
		return err
	}
	if oid, ok := res.InsertedID.(primitive.ObjectID); ok {
		m.ID = oid.Hex()
	}
	return nil
}

func (r *MensagemRepo) ListarPorConversa(ctx context.Context, conversaID string, limite int64, antes *time.Time) ([]*model.Mensagem, error) {
	filter := bson.M{"conversaId": conversaID}
	if antes != nil {
		filter["enviadaEm"] = bson.M{"$lt": *antes}
	}
	opts := options.Find().SetSort(bson.M{"enviadaEm": -1}).SetLimit(limite)
	cur, err := r.coll.Find(ctx, filter, opts)
	if err != nil {
		return nil, err
	}
	defer cur.Close(ctx)
	var list []*model.Mensagem
	if err := cur.All(ctx, &list); err != nil {
		return nil, err
	}
	for i, j := 0, len(list)-1; i < j; i, j = i+1, j-1 {
		list[i], list[j] = list[j], list[i]
	}
	return list, nil
}
