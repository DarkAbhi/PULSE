package notification

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5"
)

type notificationStore interface {
	List(context.Context, int64, int) ([]Notification, error)
	Dismiss(context.Context, int64, int64) (bool, error)
	Clear(context.Context, int64) error
	CreateGymReminder(context.Context, pgx.Tx, int64, string) (int64, error)
	GymReminderExists(context.Context, pgx.Tx, int64, int64) (bool, error)
	DismissGymReminder(context.Context, pgx.Tx, int64) error
	CreateAirFillReminder(context.Context, pgx.Tx, int64, int64, string) (int64, error)
}

type Service struct{ store notificationStore }

func NewService(store notificationStore) *Service { return &Service{store: store} }

func (s *Service) List(ctx context.Context, userID int64, limit int) ([]Notification, error) {
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
func (s *Service) CreateGymReminder(ctx context.Context, tx pgx.Tx, userID int64, date string) (int64, error) {
	id, err := s.store.CreateGymReminder(ctx, tx, userID, date)
	if err != nil {
		return 0, fmt.Errorf("create gym reminder: %w", err)
	}
	return id, nil
}
func (s *Service) GymReminderExists(ctx context.Context, tx pgx.Tx, userID, id int64) (bool, error) {
	ok, err := s.store.GymReminderExists(ctx, tx, userID, id)
	if err != nil {
		return false, fmt.Errorf("check gym reminder: %w", err)
	}
	return ok, nil
}
func (s *Service) DismissGymReminder(ctx context.Context, tx pgx.Tx, id int64) error {
	if err := s.store.DismissGymReminder(ctx, tx, id); err != nil {
		return fmt.Errorf("dismiss gym reminder: %w", err)
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
