package activity

import (
	"context"
	"errors"
	"time"

	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"
)

type Repository struct{ db *pgxpool.Pool }

func NewRepository(db *pgxpool.Pool) *Repository { return &Repository{db: db} }

func (r *Repository) MeditatedToday(ctx context.Context, start, end time.Time) (bool, error) {
	var exists bool
	err := r.db.QueryRow(ctx, `SELECT EXISTS(SELECT 1 FROM meditations WHERE created_at >= $1 AND created_at < $2)`, start, end).Scan(&exists)
	return exists, err
}

func (r *Repository) AddMeditation(ctx context.Context) error {
	_, err := r.db.Exec(ctx, `INSERT INTO meditations DEFAULT VALUES`)
	return err
}

func (r *Repository) AddSport(ctx context.Context, name string) error {
	_, err := r.db.Exec(ctx, `INSERT INTO sports (name) VALUES ($1)`, name)
	var pgErr *pgconn.PgError
	if errors.As(err, &pgErr) && pgErr.Code == "23514" {
		return ErrInvalidSport
	}
	return err
}
