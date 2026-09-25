package purchase

import (
	"context"
	"fmt"

	"github.com/DarkAbhi/life-backend/internal/timeutil"
)

type purchaseStore interface {
	List(context.Context, int64, string) ([]Purchase, error)
	Create(context.Context, int64, string, Purchase) (Purchase, error)
	Delete(context.Context, int64, int64, string) (bool, error)
	Clear(context.Context, int64, string) error
}

type Service struct{ store purchaseStore }

func NewService(store purchaseStore) *Service { return &Service{store: store} }

func (s *Service) List(ctx context.Context, userID int64) ([]Purchase, float64, error) {
	items, err := s.store.List(ctx, userID, timeutil.NextMonthDate())
	if err != nil {
		return nil, 0, fmt.Errorf("list purchases: %w", err)
	}
	var total float64
	for _, item := range items {
		total += item.Price
	}
	return items, total, nil
}

func (s *Service) Create(ctx context.Context, userID int64, name string, price float64, url *string) (Purchase, error) {
	item, err := NewPurchase(name, price, url)
	if err != nil {
		return Purchase{}, err
	}
	saved, err := s.store.Create(ctx, userID, timeutil.NextMonthDate(), item)
	if err != nil {
		return Purchase{}, fmt.Errorf("create purchase: %w", err)
	}
	return saved, nil
}

func (s *Service) Delete(ctx context.Context, userID, id int64) (bool, error) {
	deleted, err := s.store.Delete(ctx, userID, id, timeutil.NextMonthDate())
	if err != nil {
		return false, fmt.Errorf("delete purchase: %w", err)
	}
	return deleted, nil
}

func (s *Service) Clear(ctx context.Context, userID int64) error {
	if err := s.store.Clear(ctx, userID, timeutil.NextMonthDate()); err != nil {
		return fmt.Errorf("clear purchases: %w", err)
	}
	return nil
}
