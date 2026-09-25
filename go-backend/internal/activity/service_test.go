package activity

import (
	"context"
	"errors"
	"testing"
	"time"
)

type stubStore struct {
	meditated   bool
	meditations int
	sports      []string
}

func (s *stubStore) MeditatedToday(context.Context, time.Time, time.Time) (bool, error) {
	return s.meditated, nil
}
func (s *stubStore) AddMeditation(context.Context) error { s.meditations++; return nil }
func (s *stubStore) AddSport(_ context.Context, name string) error {
	s.sports = append(s.sports, name)
	return nil
}

func TestService(t *testing.T) {
	store := &stubStore{}
	service := NewService(store)
	if err := service.AddMeditation(context.Background(), time.Now()); err != nil || store.meditations != 1 {
		t.Fatalf("add meditation: %v, count %d", err, store.meditations)
	}
	store.meditated = true
	if err := service.AddMeditation(context.Background(), time.Now()); !errors.Is(err, ErrAlreadyMeditated) || store.meditations != 1 {
		t.Fatalf("duplicate meditation: %v, count %d", err, store.meditations)
	}
	if err := service.AddSport(context.Background(), "tennis"); !errors.Is(err, ErrInvalidSport) {
		t.Fatalf("invalid sport: %v", err)
	}
	if err := service.AddSport(context.Background(), "cricket"); err != nil || len(store.sports) != 1 {
		t.Fatalf("add sport: %v, sports %v", err, store.sports)
	}
}
