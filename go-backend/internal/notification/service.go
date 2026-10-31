package notification

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5"
)

type store interface {
	List(context.Context, int64, int) ([]Item, error)
	Dismiss(context.Context, int64, int64) (bool, error)
	Clear(context.Context, int64) error
	CreateAirFillReminder(context.Context, pgx.Tx, int64, int64, string) (int64, error)
}

type Service struct{ store store }

func NewService(store store) *Service { return &Service{store: store} }

func (s *Service) List(ctx context.Context, userID int64, limit int) ([]Item, error) {
	items, err := s.store.List(ctx, userID, limit)
	if err != nil {
		return nil, fmt.Errorf("list notifications: %w", err)
	}
	return items, nil
}
func (s *Service) Dismiss(ctx context.Context, userID, id int64) (bool, error) {
	ok, err := s.store.Dismiss(ctx, userID, id)
	if err != nil {
		return false, fmt.Errorf("dismiss notification: %w", err)
	}
	return ok, nil
}
func (s *Service) Clear(ctx context.Context, userID int64) error {
	if err := s.store.Clear(ctx, userID); err != nil {
		return fmt.Errorf("clear notifications: %w", err)
	}
	return nil
}
func (s *Service) CreateAirFillReminder(ctx context.Context, tx pgx.Tx, userID, vehicleID int64, name string) (int64, error) {
	id, err := s.store.CreateAirFillReminder(ctx, tx, userID, vehicleID, name)
	if err != nil {
		return 0, fmt.Errorf("create air-fill reminder: %w", err)
	}
	return id, nil
}
