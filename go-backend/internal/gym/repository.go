package gym

import (
	"context"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type Repository struct{ db *pgxpool.Pool }

func NewRepository(db *pgxpool.Pool) *Repository { return &Repository{db: db} }

func (r *Repository) VisitToday(ctx context.Context, start, end time.Time) (int64, error) {
	var id int64
	err := r.db.QueryRow(ctx, `SELECT id FROM gym_visits WHERE created_at >= $1 AND created_at < $2 ORDER BY created_at DESC LIMIT 1`, start, end).Scan(&id)
	return id, err
}
func (r *Repository) AddVisit(ctx context.Context) (int64, error) {
	var id int64
	err := r.db.QueryRow(ctx, `INSERT INTO gym_visits DEFAULT VALUES RETURNING id`).Scan(&id)
	return id, err
}
func (r *Repository) ListVisits(ctx context.Context) ([]gymVisitListItem, error) {
	rows, err := r.db.Query(ctx, `SELECT id,created_at FROM gym_visits ORDER BY created_at DESC,id DESC`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	items := make([]gymVisitListItem, 0)
	for rows.Next() {
		var item gymVisitListItem
		if err := rows.Scan(&item.ID, &item.CreatedAt); err != nil {
			return nil, err
		}
		item.CreatedAt = item.CreatedAt.UTC()
		items = append(items, item)
	}
	return items, rows.Err()
}
func (r *Repository) DeleteVisit(ctx context.Context, id int64) (bool, error) {
	tag, err := r.db.Exec(ctx, `DELETE FROM gym_visits WHERE id=$1`, id)
	return tag.RowsAffected() > 0, err
}
func (r *Repository) VisitExists(ctx context.Context, id int64) (bool, error) {
	var exists bool
	err := r.db.QueryRow(ctx, `SELECT EXISTS(SELECT 1 FROM gym_visits WHERE id=$1)`, id).Scan(&exists)
	return exists, err
}
func (r *Repository) ListExercises(ctx context.Context, visitID int64) ([]gymExerciseDTO, error) {
	rows, err := r.db.Query(ctx, `SELECT id,name FROM gym_visit_exercises WHERE gym_visit_id=$1 ORDER BY id ASC`, visitID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	items := make([]gymExerciseDTO, 0)
	for rows.Next() {
		var item gymExerciseDTO
		if err := rows.Scan(&item.ID, &item.Name); err != nil {
			return nil, err
		}
		sets, err := r.listSets(ctx, item.ID)
		if err != nil {
			return nil, err
		}
		item.Sets = sets
		items = append(items, item)
	}
	return items, rows.Err()
}
func (r *Repository) listSets(ctx context.Context, exerciseID int64) ([]gymExerciseSetDTO, error) {
	rows, err := r.db.Query(ctx, `SELECT id,set_number,reps,weight FROM gym_exercise_sets WHERE gym_visit_exercise_id=$1 ORDER BY set_number ASC`, exerciseID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	items := make([]gymExerciseSetDTO, 0)
	for rows.Next() {
		var item gymExerciseSetDTO
		if err := rows.Scan(&item.ID, &item.SetNumber, &item.Reps, &item.Weight); err != nil {
			return nil, err
		}
		items = append(items, item)
	}
	return items, rows.Err()
}
func (r *Repository) CreateExercise(ctx context.Context, visitID int64, name string, sets []exerciseSetInput) (gymExerciseDTO, error) {
	tx, err := r.db.Begin(ctx)
	if err != nil {
		return gymExerciseDTO{}, err
	}
	defer tx.Rollback(ctx)
	var item gymExerciseDTO
	if err := tx.QueryRow(ctx, `INSERT INTO gym_visit_exercises (gym_visit_id,name) VALUES ($1,$2) RETURNING id,name`, visitID, name).Scan(&item.ID, &item.Name); err != nil {
		return gymExerciseDTO{}, err
	}
	item.Sets = make([]gymExerciseSetDTO, 0, len(sets))
	for i, set := range sets {
		var saved gymExerciseSetDTO
		if err := tx.QueryRow(ctx, `INSERT INTO gym_exercise_sets (gym_visit_exercise_id,set_number,reps,weight) VALUES ($1,$2,$3,$4) RETURNING id,set_number,reps,weight`, item.ID, i+1, set.Reps, set.Weight).Scan(&saved.ID, &saved.SetNumber, &saved.Reps, &saved.Weight); err != nil {
			return gymExerciseDTO{}, err
		}
		item.Sets = append(item.Sets, saved)
	}
	if err := tx.Commit(ctx); err != nil {
		return gymExerciseDTO{}, err
	}
	return item, nil
}
func (r *Repository) Begin(ctx context.Context) (pgx.Tx, error) { return r.db.Begin(ctx) }
func (r *Repository) AddVisitTx(ctx context.Context, tx pgx.Tx) (int64, error) {
	var id int64
	err := tx.QueryRow(ctx, `INSERT INTO gym_visits DEFAULT VALUES RETURNING id`).Scan(&id)
	return id, err
}
func (r *Repository) DeliveryExists(ctx context.Context, tx pgx.Tx, userID int64, date string) (bool, error) {
	var exists bool
	err := tx.QueryRow(ctx, `SELECT EXISTS(SELECT 1 FROM gym_reminder_deliveries WHERE user_id=$1 AND reminder_date=$2::date)`, userID, date).Scan(&exists)
	return exists, err
}
func (r *Repository) RecordDelivery(ctx context.Context, tx pgx.Tx, userID int64, date string, notificationID int64) error {
	_, err := tx.Exec(ctx, `INSERT INTO gym_reminder_deliveries (user_id,reminder_date,notification_id) VALUES ($1,$2::date,$3)`, userID, date, notificationID)
	return err
}
