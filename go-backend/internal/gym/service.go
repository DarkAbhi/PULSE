package gym

import (
	"context"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/jackc/pgx/v5"

	"github.com/DarkAbhi/life-backend/internal/timeutil"
)

var ErrVisitNotFound = errors.New("gym: visit not found")
var ErrExerciseNotFound = errors.New("gym: exercise not found")

type ValidationError struct{ Message string }

func (e ValidationError) Error() string { return e.Message }

type store interface {
	OverviewData(context.Context, int64, time.Time, time.Time) (overviewData, error)
	VisitToday(context.Context, int64, time.Time, time.Time) (int64, error)
	AddVisit(context.Context, int64) (int64, error)
	AddVisitOnDate(context.Context, int64, time.Time) (int64, error)
	ListVisits(context.Context, int64) ([]visitListItem, error)
	GetVisit(context.Context, int64, int64) (visitDetail, error)
	DeleteVisit(context.Context, int64, int64) (bool, error)
	DeleteExercise(context.Context, int64, int64, int64) (bool, error)
	UpdateExercise(context.Context, int64, int64, int64, createExerciseBody) error
	HasVisit(context.Context, int64, int64) (bool, error)
	ListExercises(context.Context, int64) ([]exerciseDTO, error)
	CreateExercises(context.Context, int64, int64, []createExerciseBody) ([]exerciseDTO, error)
	SearchCatalog(context.Context, int64, string) ([]catalogCandidate, error)
	GetCatalogExercise(context.Context, string) (catalogExercise, error)
}

type Service struct {
	store store
}

func NewService(store store) *Service {
	return &Service{store: store}
}

func (s *Service) VisitedToday(ctx context.Context, userID int64, now time.Time) (int64, bool, error) {
	start, end := timeutil.DayBoundsIndia(now.UTC())
	id, err := s.store.VisitToday(ctx, userID, start, end)
	if errors.Is(err, pgx.ErrNoRows) {
		return 0, false, nil
	}
	if err != nil {
		return 0, false, fmt.Errorf("get gym visit today: %w", err)
	}
	return id, true, nil
}
func (s *Service) AddVisit(ctx context.Context, userID int64) (int64, error) {
	id, err := s.store.AddVisit(ctx, userID)
	if err != nil {
		return 0, fmt.Errorf("add gym visit: %w", err)
	}
	return id, nil
}
func (s *Service) AddVisitOnDate(ctx context.Context, userID int64, date string) (int64, error) {
	location, err := time.LoadLocation(timeutil.IndiaTimeZone)
	if err != nil {
		return 0, fmt.Errorf("load workout timezone: %w", err)
	}
	createdAt, err := time.ParseInLocation("2006-01-02", date, location)
	if err != nil {
		return 0, ValidationError{"date must be a valid date in YYYY-MM-DD format"}
	}
	now := time.Now().In(location)
	if date == now.Format("2006-01-02") {
		createdAt = now
	}
	id, err := s.store.AddVisitOnDate(ctx, userID, createdAt.UTC())
	if err != nil {
		return 0, fmt.Errorf("add gym visit on date: %w", err)
	}
	return id, nil
}

func (s *Service) ListVisits(ctx context.Context, userID int64) ([]visitListItem, error) {
	items, err := s.store.ListVisits(ctx, userID)
	if err != nil {
		return nil, fmt.Errorf("list gym visits: %w", err)
	}
	return items, nil
}
func (s *Service) GetVisit(ctx context.Context, userID, id int64) (visitDetail, error) {
	item, err := s.store.GetVisit(ctx, userID, id)
	if errors.Is(err, pgx.ErrNoRows) {
		return visitDetail{}, ErrVisitNotFound
	}
	if err != nil {
		return visitDetail{}, fmt.Errorf("get gym visit: %w", err)
	}
	return item, nil
}

func (s *Service) DeleteVisit(ctx context.Context, userID, id int64) error {
	ok, err := s.store.DeleteVisit(ctx, userID, id)
	if err != nil {
		return fmt.Errorf("delete gym visit: %w", err)
	}
	if !ok {
		return ErrVisitNotFound
	}
	return nil
}
func (s *Service) DeleteExercise(ctx context.Context, userID, visitID, exerciseID int64) error {
	ok, err := s.store.DeleteExercise(ctx, userID, visitID, exerciseID)
	if err != nil {
		return fmt.Errorf("delete gym exercise: %w", err)
	}
	if !ok {
		return ErrExerciseNotFound
	}
	return nil
}

func (s *Service) UpdateExercise(ctx context.Context, userID, visitID, exerciseID int64, body createExerciseBody) error {
	if err := validateExercise(&body); err != nil {
		return err
	}
	// An explicit null clears the saved match; updates never infer a replacement.
	if body.ExerciseCatalogID != nil {
		if err := s.resolveExercise(ctx, userID, &body); err != nil {
			return err
		}
	}
	if err := s.store.UpdateExercise(ctx, userID, visitID, exerciseID, body); err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return ErrExerciseNotFound
		}
		return fmt.Errorf("update gym exercise: %w", err)
	}
	return nil
}

func (s *Service) ListExercises(ctx context.Context, userID, visitID int64) ([]exerciseDTO, error) {
	hasVisit, err := s.store.HasVisit(ctx, userID, visitID)
	if err != nil {
		return nil, fmt.Errorf("check gym visit: %w", err)
	}
	if !hasVisit {
		return nil, ErrVisitNotFound
	}
	items, err := s.store.ListExercises(ctx, visitID)
	if err != nil {
		return nil, fmt.Errorf("list gym exercises: %w", err)
	}
	return items, nil
}
func (s *Service) CreateExercise(
	ctx context.Context, userID, visitID int64, body createExerciseBody,
) (exerciseDTO, error) {
	items, err := s.CreateExercises(ctx, userID, visitID, []createExerciseBody{body})
	if err != nil {
		return exerciseDTO{}, err
	}
	return items[0], nil
}

func validateExercise(body *createExerciseBody) error {
	name := strings.TrimSpace(body.Name)
	if name == "" || len([]rune(name)) > 100 {
		return ValidationError{"exercise name must be between 1 and 100 characters"}
	}
	if len(body.Sets) == 0 || len(body.Sets) > 20 {
		return ValidationError{"provide between 1 and 20 sets"}
	}
	for _, set := range body.Sets {
		if set.Reps <= 0 || set.Reps > 1000 || (set.Weight != nil && *set.Weight < 0) {
			return ValidationError{"each set needs positive reps and a non-negative weight"}
		}
	}
	body.Name = name
	if body.ExerciseCatalogID != nil {
		id := *body.ExerciseCatalogID
		if strings.TrimSpace(id) == "" || len(id) > 200 {
			return ValidationError{"exercise_catalog_id must be a non-empty catalogue ID"}
		}
	}
	if body.RememberAlias && body.ExerciseCatalogID == nil {
		return ValidationError{"choose a catalogue exercise before remembering its alias"}
	}
	return nil
}

func (s *Service) CreateExercises(
	ctx context.Context, userID, visitID int64, bodies []createExerciseBody,
) ([]exerciseDTO, error) {
	hasVisit, err := s.store.HasVisit(ctx, userID, visitID)
	if err != nil {
		return nil, fmt.Errorf("check gym visit: %w", err)
	}
	if !hasVisit {
		return nil, ErrVisitNotFound
	}
	if len(bodies) == 0 || len(bodies) > 20 {
		return nil, ValidationError{"provide between 1 and 20 exercises"}
	}
	for i := range bodies {
		if err := validateExercise(&bodies[i]); err != nil {
			return nil, err
		}
		if err := s.resolveExercise(ctx, userID, &bodies[i]); err != nil {
			return nil, err
		}
	}
	items, err := s.store.CreateExercises(ctx, userID, visitID, bodies)
	if err != nil {
		return nil, fmt.Errorf("create gym exercises: %w", err)
	}
	return items, nil
}

func normalizeExerciseName(name string) string {
	return strings.ToLower(strings.Join(strings.Fields(name), " "))
}

func (s *Service) SearchCatalog(ctx context.Context, userID int64, name string) (catalogSearch, error) {
	name = strings.TrimSpace(name)
	if name == "" || len([]rune(name)) > 100 {
		return catalogSearch{}, ValidationError{"search must be between 1 and 100 characters"}
	}
	candidates, err := s.store.SearchCatalog(ctx, userID, normalizeExerciseName(name))
	if err != nil {
		return catalogSearch{}, fmt.Errorf("search exercise catalogue: %w", err)
	}
	return catalogSearchResult(name, candidates), nil
}

func catalogSearchResult(name string, candidates []catalogCandidate) catalogSearch {
	result := catalogSearch{Query: name, MatchType: "none", Candidates: candidates}
	if len(candidates) == 0 {
		return result
	}
	result.MatchType = "suggested"
	for _, matchType := range []string{"exact", "alias"} {
		matches := []string{}
		for _, candidate := range candidates {
			if candidate.MatchType == matchType {
				matches = append(matches, candidate.ID)
			}
		}
		if len(matches) > 1 {
			result.MatchType = "ambiguous"
			return result
		}
		if len(matches) == 1 {
			result.MatchType = matchType
			result.ExerciseCatalogID = &matches[0]
			return result
		}
	}
	return result
}

func (s *Service) resolveExercise(ctx context.Context, userID int64, body *createExerciseBody) error {
	if body.ExerciseCatalogID != nil {
		_, err := s.store.GetCatalogExercise(ctx, *body.ExerciseCatalogID)
		if errors.Is(err, pgx.ErrNoRows) {
			return ValidationError{"exercise_catalog_id was not found in the catalogue"}
		}
		if err != nil {
			return fmt.Errorf("check exercise catalogue ID: %w", err)
		}
		if !body.RememberAlias {
			return nil
		}
	}
	result, err := s.SearchCatalog(ctx, userID, body.Name)
	if err != nil {
		return err
	}
	if body.RememberAlias {
		if result.MatchType == "exact" && *result.ExerciseCatalogID != *body.ExerciseCatalogID {
			return ValidationError{"this alias is already an exact name for another catalogue exercise"}
		}
		return nil
	}
	// Suggested matches require an explicit choice; never infer an ID from similarity.
	body.ExerciseCatalogID = result.ExerciseCatalogID
	return nil
}
