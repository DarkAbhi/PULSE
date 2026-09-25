package activity

import (
	"context"
	"fmt"
	"time"

	"github.com/DarkAbhi/life-backend/internal/timeutil"
)

type activityStore interface {
	MeditatedToday(context.Context, time.Time, time.Time) (bool, error)
	AddMeditation(context.Context) error
	AddSport(context.Context, string) error
}

type Service struct{ store activityStore }

func NewService(store activityStore) *Service { return &Service{store: store} }

func (s *Service) AddMeditation(ctx context.Context, now time.Time) error {
	start, end := timeutil.DayBoundsIndia(now.UTC())
	exists, err := s.store.MeditatedToday(ctx, start, end)
	if err != nil {
		return fmt.Errorf("check meditation: %w", err)
	}
	if exists {
		return ErrAlreadyMeditated
	}
	if err := s.store.AddMeditation(ctx); err != nil {
		return fmt.Errorf("add meditation: %w", err)
	}
	return nil
}

func (s *Service) AddSport(ctx context.Context, name string) error {
	switch name {
	case "cricket", "football", "badminton":
	default:
		return ErrInvalidSport
	}
	if err := s.store.AddSport(ctx, name); err != nil {
		return fmt.Errorf("add sport: %w", err)
	}
	return nil
}
