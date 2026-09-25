package purchase

import (
	"context"

	"github.com/jackc/pgx/v5/pgxpool"
)

type Repository struct{ db *pgxpool.Pool }

func NewRepository(db *pgxpool.Pool) *Repository { return &Repository{db: db} }

func (r *Repository) List(ctx context.Context, userID int64, month string) ([]Purchase, error) {
	rows, err := r.db.Query(ctx, `SELECT id, name, price, url FROM next_month_purchases WHERE user_id=$1 AND target_month=$2::date ORDER BY created_at DESC, id DESC`, userID, month)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	items := make([]Purchase, 0)
	for rows.Next() {
		var item Purchase
		if err := rows.Scan(&item.ID, &item.Name, &item.Price, &item.URL); err != nil {
			return nil, err
		}
		items = append(items, item)
	}
	return items, rows.Err()
}

func (r *Repository) Create(ctx context.Context, userID int64, month string, item Purchase) (Purchase, error) {
	var saved Purchase
	err := r.db.QueryRow(ctx, `INSERT INTO next_month_purchases (user_id,target_month,name,price,url) VALUES ($1,$2::date,$3,$4,$5) RETURNING id,name,price,url`, userID, month, item.Name, item.Price, item.URL).Scan(&saved.ID, &saved.Name, &saved.Price, &saved.URL)
	return saved, err
}

func (r *Repository) Delete(ctx context.Context, userID, id int64, month string) (bool, error) {
	tag, err := r.db.Exec(ctx, `DELETE FROM next_month_purchases WHERE id=$1 AND user_id=$2 AND target_month=$3::date`, id, userID, month)
	return tag.RowsAffected() > 0, err
}

func (r *Repository) Clear(ctx context.Context, userID int64, month string) error {
	_, err := r.db.Exec(ctx, `DELETE FROM next_month_purchases WHERE user_id=$1 AND target_month=$2::date`, userID, month)
	return err
}
