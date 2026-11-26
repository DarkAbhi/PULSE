package auth

import (
	"context"
	"time"

	"github.com/jackc/pgx/v5/pgtype"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/DarkAbhi/life-backend/internal/auth/query"
)

type Repository struct{ queries *query.Queries }

func NewRepository(db *pgxpool.Pool) *Repository {
	return &Repository{queries: query.New(db)}
}

func (r *Repository) LoginUser(ctx context.Context, username string) (int64, string, error) {
	user, err := r.queries.LoginUser(ctx, username)
	return user.ID, user.PasswordHash, err
}

func (r *Repository) CreateSession(ctx context.Context, userID int64, hash string, expires time.Time) error {
	return r.queries.CreateSession(ctx, query.CreateSessionParams{
		UserID: userID, TokenHash: hash,
		ExpiresAt: pgtype.Timestamptz{Time: expires, Valid: true},
	})
}

func (r *Repository) DeleteSession(ctx context.Context, hash string) error {
	return r.queries.DeleteSession(ctx, hash)
}

func (r *Repository) SessionUser(ctx context.Context, hash string) (SessionUser, error) {
	user, err := r.queries.SessionUser(ctx, hash)
	return SessionUser{ID: user.ID, Username: user.Username}, err
}

func (r *Repository) PasswordHash(ctx context.Context, userID int64) (string, error) {
	return r.queries.PasswordHash(ctx, userID)
}

func (r *Repository) UpdatePasswordHash(ctx context.Context, userID int64, hash string) error {
	return r.queries.UpdatePasswordHash(ctx, query.UpdatePasswordHashParams{ID: userID, PasswordHash: hash})
}

func (r *Repository) ListUserIDs(ctx context.Context) ([]int64, error) {
	ids, err := r.queries.ListUserIDs(ctx)
	if ids == nil {
		ids = []int64{}
	}
	return ids, err
}
