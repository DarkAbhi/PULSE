package auth

import (
	"context"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

type Repository struct{ db *pgxpool.Pool }

func NewRepository(db *pgxpool.Pool) *Repository { return &Repository{db: db} }

func (r *Repository) LoginUser(ctx context.Context, username string) (int64, string, error) {
	var id int64
	var hash string
	err := r.db.QueryRow(ctx, `SELECT id,password_hash FROM users WHERE username=$1`, username).Scan(&id, &hash)
	return id, hash, err
}
func (r *Repository) CreateSession(ctx context.Context, userID int64, hash string, expires time.Time) error {
	_, err := r.db.Exec(ctx, `INSERT INTO user_sessions (user_id,token_hash,expires_at) VALUES ($1,$2,$3)`, userID, hash, expires)
	return err
}
func (r *Repository) DeleteSession(ctx context.Context, hash string) error {
	_, err := r.db.Exec(ctx, `DELETE FROM user_sessions WHERE token_hash=$1`, hash)
	return err
}
func (r *Repository) SessionUser(ctx context.Context, hash string) (SessionUser, error) {
	var user SessionUser
	err := r.db.QueryRow(ctx, `SELECT users.id,users.username FROM user_sessions JOIN users ON users.id=user_sessions.user_id WHERE user_sessions.token_hash=$1 AND user_sessions.expires_at>NOW()`, hash).Scan(&user.ID, &user.Username)
	return user, err
}
func (r *Repository) PasswordHash(ctx context.Context, userID int64) (string, error) {
	var hash string
	err := r.db.QueryRow(ctx, `SELECT password_hash FROM users WHERE id=$1`, userID).Scan(&hash)
	return hash, err
}
func (r *Repository) UpdatePasswordHash(ctx context.Context, userID int64, hash string) error {
	_, err := r.db.Exec(ctx, `UPDATE users SET password_hash=$1,updated_at=NOW() WHERE id=$2`, hash, userID)
	return err
}
func (r *Repository) ListUserIDs(ctx context.Context) ([]int64, error) {
	rows, err := r.db.Query(ctx, `SELECT id FROM users`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	ids := make([]int64, 0)
	for rows.Next() {
		var id int64
		if err := rows.Scan(&id); err != nil {
			return nil, err
		}
		ids = append(ids, id)
	}
	return ids, rows.Err()
}
