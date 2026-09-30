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

type ValidationError struct{ Message string }

func (e ValidationError) Error() string { return e.Message }

type store interface {
	VisitToday(context.Context, int64, time.Time, time.Time) (int64, error)
	AddVisit(context.Context, int64) (int64, error)
	ListVisits(context.Context, int64) ([]visitListItem, error)
	DeleteVisit(context.Context, int64, int64) (bool, error)
	HasVisit(context.Context, int64, int64) (bool, error)
	ListExercises(context.Context, int64) ([]exerciseDTO, error)
	CreateExercise(context.Context, int64, string, []exerciseSetInput) (exerciseDTO, error)
	CreateExercises(context.Context, int64, []createExerciseBody) ([]exerciseDTO, error)
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
func (s *Service) ListVisits(ctx context.Context, userID int64) ([]visitListItem, error) {
	items, err := s.store.ListVisits(ctx, userID)
	if err != nil {
		return nil, fmt.Errorf("list gym visits: %w", err)
	}
	return items, nil
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
func (s *Service) CreateExercise(ctx context.Context, userID, visitID int64, body createExerciseBody) (exerciseDTO, error) {
	hasVisit, err := s.store.HasVisit(ctx, userID, visitID)
	if err != nil {
		return exerciseDTO{}, fmt.Errorf("check gym visit: %w", err)
	}
	if !hasVisit {
		return exerciseDTO{}, ErrVisitNotFound
	}
	if err := validateExercise(&body); err != nil {
		return exerciseDTO{}, err
	}
	item, err := s.store.CreateExercise(ctx, visitID, body.Name, body.Sets)
	if err != nil {
		return exerciseDTO{}, fmt.Errorf("create gym exercise: %w", err)
	}
	return item, nil
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
	return nil
}

func (s *Service) CreateExercises(ctx context.Context, userID, visitID int64, bodies []createExerciseBody) ([]exerciseDTO, error) {
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
	}
	items, err := s.store.CreateExercises(ctx, visitID, bodies)
	if err != nil {
		return nil, fmt.Errorf("create gym exercises: %w", err)
	}
	return items, nil
}
