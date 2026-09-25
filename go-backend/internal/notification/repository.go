package notification

import (
	"context"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type Repository struct{ db *pgxpool.Pool }

func NewRepository(db *pgxpool.Pool) *Repository { return &Repository{db: db} }

func (r *Repository) List(ctx context.Context, userID int64, limit int) ([]Notification, error) {
	rows, err := r.db.Query(ctx, `SELECT id,source,title,body,target_path,priority,created_at FROM notifications WHERE user_id=$1 AND dismissed_at IS NULL ORDER BY created_at DESC,id DESC LIMIT $2`, userID, limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	items := make([]Notification, 0)
	for rows.Next() {
		var item Notification
		if err := rows.Scan(&item.ID, &item.Source, &item.Title, &item.Body, &item.TargetPath, &item.Priority, &item.CreatedAt); err != nil {
			return nil, err
		}
		items = append(items, item)
	}
	return items, rows.Err()
}

func (r *Repository) Dismiss(ctx context.Context, userID, id int64) (bool, error) {
	tag, err := r.db.Exec(ctx, `UPDATE notifications SET dismissed_at=NOW() WHERE id=$1 AND user_id=$2 AND dismissed_at IS NULL`, id, userID)
	return tag.RowsAffected() > 0, err
}

func (r *Repository) Clear(ctx context.Context, userID int64) error {
	_, err := r.db.Exec(ctx, `UPDATE notifications SET dismissed_at=NOW() WHERE user_id=$1 AND dismissed_at IS NULL`, userID)
	return err
}

// The caller owns the transaction when creating a reminder and recording its delivery.
func (r *Repository) CreateGymReminder(ctx context.Context, tx pgx.Tx, userID int64, date string) (int64, error) {
	var id int64
	err := tx.QueryRow(ctx, `INSERT INTO notifications (user_id,source,title,body,target_path,priority,metadata) VALUES ($1,'Gym reminder','Time for the gym','Your 3:30 PM gym reminder. Mark your visit when you are done.','/gym-visits',1,jsonb_build_object('reminder_date',$2::text)) RETURNING id`, userID, date).Scan(&id)
	return id, err
}

func (r *Repository) GymReminderExists(ctx context.Context, tx pgx.Tx, userID, id int64) (bool, error) {
	var exists bool
	err := tx.QueryRow(ctx, `SELECT EXISTS(SELECT 1 FROM notifications WHERE id=$1 AND user_id=$2 AND source='Gym reminder' AND dismissed_at IS NULL)`, id, userID).Scan(&exists)
	return exists, err
}

func (r *Repository) DismissGymReminder(ctx context.Context, tx pgx.Tx, id int64) error {
	_, err := tx.Exec(ctx, `UPDATE notifications SET dismissed_at=NOW() WHERE id=$1`, id)
	return err
}

func (r *Repository) CreateAirFillReminder(ctx context.Context, tx pgx.Tx, userID, vehicleID int64, name string) (int64, error) {
	var id int64
	err := tx.QueryRow(ctx, `INSERT INTO notifications (user_id,source,title,body,target_path,priority,metadata) VALUES ($1,'Garage','Time to check ' || $3 || '''s air','It has been 30 days since you last filled air in ' || $3 || '.','/garage',1,jsonb_build_object('vehicle_id',$2::bigint)) RETURNING id`, userID, vehicleID, name).Scan(&id)
	return id, err
}
