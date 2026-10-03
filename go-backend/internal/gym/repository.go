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
func (r *Repository) AddVisitOnDate(ctx context.Context, userID int64, date time.Time) (int64, error) {
	return r.queries.AddVisitOnDate(ctx, query.AddVisitOnDateParams{
		UserID:    userID,
		CreatedAt: pgtype.Timestamptz{Time: date, Valid: true},
	})
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
func (r *Repository) GetVisit(ctx context.Context, userID, id int64) (visitDetail, error) {
	row, err := r.queries.GetVisit(ctx, query.GetVisitParams{ID: id, UserID: userID})
	if err != nil {
		return visitDetail{}, err
	}
	item := visitDetail{
		visitListItem:        visitListItem{ID: row.ID, CreatedAt: row.CreatedAt.Time.UTC()},
		DurationSeconds:      row.DurationSeconds,
		CaloriesBurned:       row.CaloriesBurned,
		SyncedFromAppleWatch: row.SyncedFromAppleWatch,
	}
	if row.StartTime.Valid {
		start := row.StartTime.Time.UTC()
		item.StartTime = &start
	}
	if row.EndTime.Valid {
		end := row.EndTime.Time.UTC()
		item.EndTime = &end
	}
	return item, nil
}

func (r *Repository) DeleteVisit(ctx context.Context, userID, id int64) (bool, error) {
	count, err := r.queries.DeleteVisit(ctx, query.DeleteVisitParams{ID: id, UserID: userID})
	return count > 0, err
}
func (r *Repository) DeleteExercise(ctx context.Context, userID, visitID, exerciseID int64) (bool, error) {
	count, err := r.queries.DeleteExercise(ctx, query.DeleteExerciseParams{
		ExerciseID: exerciseID, VisitID: visitID, UserID: userID,
	})
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
		item := exerciseDTO{
			ID: row.ID, Name: row.Name, ExerciseCatalogID: row.ExerciseCatalogID,
			Sets: make([]exerciseSetDTO, 0, len(sets)),
		}
		if row.ExerciseCatalogID != nil && row.CatalogName != nil {
			item.CatalogExercise = &catalogExercise{
				ID: *row.ExerciseCatalogID, Name: *row.CatalogName, Equipment: row.Equipment, Data: row.Data,
			}
		}
		for _, set := range sets {
			item.Sets = append(item.Sets, exerciseSetDTO{
				ID: set.ID, SetNumber: int(set.SetNumber), Reps: int(set.Reps), Weight: set.Weight,
			})
		}
		items = append(items, item)
	}
	return items, nil
}
func (r *Repository) CreateExercises(
	ctx context.Context, userID, visitID int64, bodies []createExerciseBody,
) ([]exerciseDTO, error) {
	tx, err := r.db.Begin(ctx)
	if err != nil {
		return nil, err
	}
	defer tx.Rollback(ctx)
	q := r.queries.WithTx(tx)
	items := make([]exerciseDTO, 0, len(bodies))
	for _, body := range bodies {
		row, err := q.CreateExercise(ctx, query.CreateExerciseParams{
			GymVisitID: visitID, Name: body.Name, ExerciseCatalogID: body.ExerciseCatalogID,
		})
		if err != nil {
			return nil, err
		}
		item := exerciseDTO{
			ID: row.ID, Name: row.Name, ExerciseCatalogID: row.ExerciseCatalogID,
			Sets: make([]exerciseSetDTO, 0, len(body.Sets)),
		}
		if row.ExerciseCatalogID != nil {
			catalog, err := q.GetCatalogExercise(ctx, *row.ExerciseCatalogID)
			if err != nil {
				return nil, err
			}
			item.CatalogExercise = &catalogExercise{
				ID: catalog.ID, Name: catalog.Name, Equipment: catalog.Equipment, Data: catalog.Data,
			}
		}
		if body.RememberAlias {
			if err := q.RememberExerciseAlias(ctx, query.RememberExerciseAliasParams{
				UserID: userID, NormalizedAlias: normalizeExerciseName(body.Name), ExerciseCatalogID: *body.ExerciseCatalogID,
			}); err != nil {
				return nil, err
			}
		}
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

func (r *Repository) SearchCatalog(ctx context.Context, userID int64, name string) ([]catalogCandidate, error) {
	rows, err := r.queries.SearchExerciseCatalog(ctx, query.SearchExerciseCatalogParams{UserID: userID, Search: name})
	if err != nil {
		return nil, err
	}
	items := make([]catalogCandidate, 0, len(rows))
	for _, row := range rows {
		items = append(items, catalogCandidate{
			catalogExercise: catalogExercise{ID: row.ID, Name: row.Name, Equipment: row.Equipment, Data: row.Data},
			MatchType:       row.MatchType,
		})
	}
	return items, nil
}

func (r *Repository) GetCatalogExercise(ctx context.Context, id string) (catalogExercise, error) {
	row, err := r.queries.GetCatalogExercise(ctx, id)
	if err != nil {
		return catalogExercise{}, err
	}
	return catalogExercise{ID: row.ID, Name: row.Name, Equipment: row.Equipment, Data: row.Data}, nil
}
