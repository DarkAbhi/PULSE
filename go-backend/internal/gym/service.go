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
var ErrReminderNotFound = errors.New("gym: reminder not found")

type ValidationError struct{ Message string }

func (e ValidationError) Error() string { return e.Message }

type store interface {
	VisitToday(context.Context, time.Time, time.Time) (int64, error)
	AddVisit(context.Context) (int64, error)
	ListVisits(context.Context) ([]visitListItem, error)
	DeleteVisit(context.Context, int64) (bool, error)
	HasVisit(context.Context, int64) (bool, error)
	ListExercises(context.Context, int64) ([]exerciseDTO, error)
	CreateExercise(context.Context, int64, string, []exerciseSetInput) (exerciseDTO, error)
	Begin(context.Context) (pgx.Tx, error)
	AddVisitTx(context.Context, pgx.Tx) (int64, error)
	HasDelivery(context.Context, pgx.Tx, int64, string) (bool, error)
	RecordDelivery(context.Context, pgx.Tx, int64, string, int64) error
}
type reminderStore interface {
	CreateGymReminder(context.Context, pgx.Tx, int64, string) (int64, error)
	HasGymReminder(context.Context, pgx.Tx, int64, int64) (bool, error)
	DismissGymReminder(context.Context, pgx.Tx, int64) error
}
type userLister interface {
	ListUserIDs(context.Context) ([]int64, error)
}

type Service struct {
	store     store
	reminders reminderStore
	users     userLister
}

func NewService(store store, reminders reminderStore, users userLister) *Service {
	return &Service{store: store, reminders: reminders, users: users}
}

func (s *Service) VisitedToday(ctx context.Context, now time.Time) (int64, bool, error) {
	start, end := timeutil.DayBoundsIndia(now.UTC())
	id, err := s.store.VisitToday(ctx, start, end)
	if errors.Is(err, pgx.ErrNoRows) {
		return 0, false, nil
	}
	if err != nil {
		return 0, false, fmt.Errorf("get gym visit today: %w", err)
	}
	return id, true, nil
}
func (s *Service) AddVisit(ctx context.Context) (int64, error) {
	id, err := s.store.AddVisit(ctx)
	if err != nil {
		return 0, fmt.Errorf("add gym visit: %w", err)
	}
	return id, nil
}
func (s *Service) ListVisits(ctx context.Context) ([]visitListItem, error) {
	items, err := s.store.ListVisits(ctx)
	if err != nil {
		return nil, fmt.Errorf("list gym visits: %w", err)
	}
	return items, nil
}
func (s *Service) DeleteVisit(ctx context.Context, id int64) error {
	ok, err := s.store.DeleteVisit(ctx, id)
	if err != nil {
		return fmt.Errorf("delete gym visit: %w", err)
	}
	if !ok {
		return ErrVisitNotFound
	}
	return nil
}
func (s *Service) ListExercises(ctx context.Context, visitID int64) ([]exerciseDTO, error) {
	hasVisit, err := s.store.HasVisit(ctx, visitID)
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
func (s *Service) CreateExercise(ctx context.Context, visitID int64, body createExerciseBody) (exerciseDTO, error) {
	hasVisit, err := s.store.HasVisit(ctx, visitID)
	if err != nil {
		return exerciseDTO{}, fmt.Errorf("check gym visit: %w", err)
	}
	if !hasVisit {
		return exerciseDTO{}, ErrVisitNotFound
	}
	name := strings.TrimSpace(body.Name)
	if name == "" || len([]rune(name)) > 100 {
		return exerciseDTO{}, ValidationError{"exercise name must be between 1 and 100 characters"}
	}
	if len(body.Sets) == 0 || len(body.Sets) > 20 {
		return exerciseDTO{}, ValidationError{"provide between 1 and 20 sets"}
	}
	for _, set := range body.Sets {
		if set.Reps <= 0 || set.Reps > 1000 || (set.Weight != nil && *set.Weight < 0) {
			return exerciseDTO{}, ValidationError{"each set needs positive reps and a non-negative weight"}
		}
	}
	item, err := s.store.CreateExercise(ctx, visitID, name, body.Sets)
	if err != nil {
		return exerciseDTO{}, fmt.Errorf("create gym exercise: %w", err)
	}
	return item, nil
}
func (s *Service) MarkReminderVisited(ctx context.Context, userID, notificationID int64) (int64, error) {
	tx, err := s.store.Begin(ctx)
	if err != nil {
		return 0, fmt.Errorf("begin reminder visit: %w", err)
	}
	defer tx.Rollback(ctx)
	hasReminder, err := s.reminders.HasGymReminder(ctx, tx, userID, notificationID)
	if err != nil {
		return 0, err
	}
	if !hasReminder {
		return 0, ErrReminderNotFound
	}
	visitID, err := s.store.AddVisitTx(ctx, tx)
	if err != nil {
		return 0, fmt.Errorf("add reminder visit: %w", err)
	}
	if err := s.reminders.DismissGymReminder(ctx, tx, notificationID); err != nil {
		return 0, err
	}
	if err := tx.Commit(ctx); err != nil {
		return 0, fmt.Errorf("commit reminder visit: %w", err)
	}
	return visitID, nil
}
func (s *Service) CreateDueReminders(ctx context.Context, now time.Time) error {
	location, err := time.LoadLocation(timeutil.IndiaTimeZone)
	if err != nil {
		return fmt.Errorf("load gym timezone: %w", err)
	}
	local := now.In(location)
	if local.Weekday() == time.Sunday || local.Hour() < 15 || (local.Hour() == 15 && local.Minute() < 30) {
		return nil
	}
	date := local.Format("2006-01-02")
	users, err := s.users.ListUserIDs(ctx)
	if err != nil {
		return fmt.Errorf("list gym reminder users: %w", err)
	}
	var failures []error
	for _, userID := range users {
		if err := s.createReminder(ctx, userID, date); err != nil {
			failures = append(failures, fmt.Errorf("create gym reminder for user %d: %w", userID, err))
		}
	}
	return errors.Join(failures...)
}
func (s *Service) createReminder(ctx context.Context, userID int64, date string) error {
	tx, err := s.store.Begin(ctx)
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx)
	hasDelivery, err := s.store.HasDelivery(ctx, tx, userID, date)
	if err != nil {
		return err
	}
	if hasDelivery {
		return nil
	}
	id, err := s.reminders.CreateGymReminder(ctx, tx, userID, date)
	if err != nil {
		return err
	}
	if err := s.store.RecordDelivery(ctx, tx, userID, date, id); err != nil {
		return err
	}
	return tx.Commit(ctx)
}
