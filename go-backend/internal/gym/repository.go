package gym

import (
	"context"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgtype"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/DarkAbhi/life-backend/internal/gym/query"
)

type Repository struct {
	db      *pgxpool.Pool
	queries *query.Queries
}

func NewRepository(db *pgxpool.Pool) *Repository {
	return &Repository{db: db, queries: query.New(db)}
}

func (r *Repository) VisitToday(ctx context.Context, start, end time.Time) (int64, error) {
	return r.queries.VisitToday(ctx, query.VisitTodayParams{
		CreatedAt:   pgtype.Timestamptz{Time: start, Valid: true},
		CreatedAt_2: pgtype.Timestamptz{Time: end, Valid: true},
	})
}
func (r *Repository) AddVisit(ctx context.Context) (int64, error) { return r.queries.AddVisit(ctx) }
func (r *Repository) ListVisits(ctx context.Context) ([]visitListItem, error) {
	rows, err := r.queries.ListVisits(ctx)
	if err != nil {
		return nil, err
	}
	items := make([]visitListItem, 0, len(rows))
	for _, row := range rows {
		items = append(items, visitListItem{ID: row.ID, CreatedAt: row.CreatedAt.Time.UTC()})
	}
	return items, nil
}
func (r *Repository) DeleteVisit(ctx context.Context, id int64) (bool, error) {
	count, err := r.queries.DeleteVisit(ctx, id)
	return count > 0, err
}
func (r *Repository) HasVisit(ctx context.Context, id int64) (bool, error) {
	return r.queries.VisitExists(ctx, id)
}
func (r *Repository) ListExercises(ctx context.Context, visitID int64) ([]exerciseDTO, error) {
	rows, err := r.queries.ListExercises(ctx, visitID)
	if err != nil {
		return nil, err
	}
	items := make([]exerciseDTO, 0, len(rows))
	for _, row := range rows {
		sets, err := r.queries.ListSets(ctx, row.ID)
		if err != nil {
			return nil, err
		}
		item := exerciseDTO{ID: row.ID, Name: row.Name, Sets: make([]exerciseSetDTO, 0, len(sets))}
		for _, set := range sets {
			item.Sets = append(item.Sets, exerciseSetDTO{
				ID: set.ID, SetNumber: int(set.SetNumber), Reps: int(set.Reps), Weight: set.Weight,
			})
		}
		items = append(items, item)
	}
	return items, nil
}
func (r *Repository) CreateExercise(ctx context.Context, visitID int64, name string, sets []exerciseSetInput) (exerciseDTO, error) {
	tx, err := r.db.Begin(ctx)
	if err != nil {
		return exerciseDTO{}, err
	}
	defer tx.Rollback(ctx)
	q := r.queries.WithTx(tx)
	row, err := q.CreateExercise(ctx, query.CreateExerciseParams{GymVisitID: visitID, Name: name})
	if err != nil {
		return exerciseDTO{}, err
	}
	item := exerciseDTO{ID: row.ID, Name: row.Name, Sets: make([]exerciseSetDTO, 0, len(sets))}
	for i, set := range sets {
		saved, err := q.CreateSet(ctx, query.CreateSetParams{
			GymVisitExerciseID: row.ID, SetNumber: int16(i + 1), Reps: int16(set.Reps), Weight: set.Weight,
		})
		if err != nil {
			return exerciseDTO{}, err
		}
		item.Sets = append(item.Sets, exerciseSetDTO{
			ID: saved.ID, SetNumber: int(saved.SetNumber), Reps: int(saved.Reps), Weight: saved.Weight,
		})
	}
	if err := tx.Commit(ctx); err != nil {
		return exerciseDTO{}, err
	}
	return item, nil
}
func (r *Repository) Begin(ctx context.Context) (pgx.Tx, error) { return r.db.Begin(ctx) }
func (r *Repository) AddVisitTx(ctx context.Context, tx pgx.Tx) (int64, error) {
	return r.queries.WithTx(tx).AddVisit(ctx)
}
func (r *Repository) HasDelivery(ctx context.Context, tx pgx.Tx, userID int64, date string) (bool, error) {
	return r.queries.WithTx(tx).DeliveryExists(ctx, query.DeliveryExistsParams{UserID: userID, Column2: date})
}
func (r *Repository) RecordDelivery(ctx context.Context, tx pgx.Tx, userID int64, date string, notificationID int64) error {
	return r.queries.WithTx(tx).RecordDelivery(ctx, query.RecordDeliveryParams{
		UserID: userID, Column2: date, NotificationID: notificationID,
	})
}
