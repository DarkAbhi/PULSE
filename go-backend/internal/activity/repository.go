package activity

import (
	"context"
	"errors"
	"time"

	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgtype"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/DarkAbhi/life-backend/internal/activity/query"
)

type Repository struct{ queries *query.Queries }

func NewRepository(db *pgxpool.Pool) *Repository {
	return &Repository{queries: query.New(db)}
}

func (r *Repository) MeditatedToday(ctx context.Context, start, end time.Time) (bool, error) {
	return r.queries.MeditatedToday(ctx, query.MeditatedTodayParams{
		CreatedAt:   pgtype.Timestamptz{Time: start, Valid: true},
		CreatedAt_2: pgtype.Timestamptz{Time: end, Valid: true},
	})
}

func (r *Repository) AddMeditation(ctx context.Context) error {
	return r.queries.AddMeditation(ctx)
}

func (r *Repository) AddSport(ctx context.Context, name string) error {
	err := r.queries.AddSport(ctx, name)
	var pgErr *pgconn.PgError
	if errors.As(err, &pgErr) && pgErr.Code == "23514" {
		return ErrInvalidSport
	}
	return err
}
