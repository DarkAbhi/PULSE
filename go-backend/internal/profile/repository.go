package profile

import (
	"context"

	"github.com/jackc/pgx/v5/pgxpool"
)

type Repository struct{ db *pgxpool.Pool }

func NewRepository(db *pgxpool.Pool) *Repository { return &Repository{db: db} }
func (r *Repository) Name(ctx context.Context, userID int64) (string, error) {
	var name string
	err := r.db.QueryRow(ctx, `SELECT display_name FROM user_profiles WHERE user_id=$1`, userID).Scan(&name)
	return name, err
}
func (r *Repository) Save(ctx context.Context, userID int64, name string) error {
	_, err := r.db.Exec(ctx, `INSERT INTO user_profiles (user_id,display_name) VALUES ($1,$2) ON CONFLICT (user_id) DO UPDATE SET display_name=EXCLUDED.display_name,updated_at=NOW()`, userID, name)
	return err
}
