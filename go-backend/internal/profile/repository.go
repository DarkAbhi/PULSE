package profile

import (
	"context"

	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/DarkAbhi/life-backend/internal/profile/query"
)

type Repository struct{ queries *query.Queries }

func NewRepository(db *pgxpool.Pool) *Repository {
	return &Repository{queries: query.New(db)}
}

func (r *Repository) Name(ctx context.Context, userID int64) (string, error) {
	return r.queries.GetDisplayName(ctx, userID)
}

func (r *Repository) Save(ctx context.Context, userID int64, name string) error {
	return r.queries.SaveDisplayName(ctx, query.SaveDisplayNameParams{UserID: userID, DisplayName: name})
}
