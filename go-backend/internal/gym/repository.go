package gym

import (
	"context"
	"time"

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

func (r *Repository) VisitToday(ctx context.Context, userID int64, start, end time.Time) (int64, error) {
	return r.queries.VisitToday(ctx, query.VisitTodayParams{
		UserID:      userID,
		CreatedAt:   pgtype.Timestamptz{Time: start, Valid: true},
		CreatedAt_2: pgtype.Timestamptz{Time: end, Valid: true},
	})
}
func (r *Repository) AddVisit(ctx context.Context, userID int64) (int64, error) {
	return r.queries.AddVisit(ctx, userID)
}
func (r *Repository) ListVisits(ctx context.Context, userID int64) ([]visitListItem, error) {
	rows, err := r.queries.ListVisits(ctx, userID)
	if err != nil {
		return nil, err
	}
	items := make([]visitListItem, 0, len(rows))
	for _, row := range rows {
		items = append(items, visitListItem{ID: row.ID, CreatedAt: row.CreatedAt.Time.UTC()})
	}
	return items, nil
}
func (r *Repository) DeleteVisit(ctx context.Context, userID, id int64) (bool, error) {
	count, err := r.queries.DeleteVisit(ctx, query.DeleteVisitParams{ID: id, UserID: userID})
	return count > 0, err
}
func (r *Repository) HasVisit(ctx context.Context, userID, id int64) (bool, error) {
	return r.queries.VisitExists(ctx, query.VisitExistsParams{ID: id, UserID: userID})
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
	items, err := r.CreateExercises(ctx, visitID, []createExerciseBody{{Name: name, Sets: sets}})
	if err != nil {
		return exerciseDTO{}, err
	}
	return items[0], nil
}

func (r *Repository) CreateExercises(ctx context.Context, visitID int64, bodies []createExerciseBody) ([]exerciseDTO, error) {
	tx, err := r.db.Begin(ctx)
	if err != nil {
		return nil, err
	}
	defer tx.Rollback(ctx)
	q := r.queries.WithTx(tx)
	items := make([]exerciseDTO, 0, len(bodies))
	for _, body := range bodies {
		row, err := q.CreateExercise(ctx, query.CreateExerciseParams{GymVisitID: visitID, Name: body.Name})
		if err != nil {
			return nil, err
		}
		item := exerciseDTO{ID: row.ID, Name: row.Name, Sets: make([]exerciseSetDTO, 0, len(body.Sets))}
		for i, set := range body.Sets {
			saved, err := q.CreateSet(ctx, query.CreateSetParams{
				GymVisitExerciseID: row.ID, SetNumber: int16(i + 1), Reps: int16(set.Reps), Weight: set.Weight,
			})
			if err != nil {
				return nil, err
			}
			item.Sets = append(item.Sets, exerciseSetDTO{
				ID: saved.ID, SetNumber: int(saved.SetNumber), Reps: int(saved.Reps), Weight: saved.Weight,
			})
		}
		items = append(items, item)
	}
	if err := tx.Commit(ctx); err != nil {
		return nil, err
	}
	return items, nil
}
