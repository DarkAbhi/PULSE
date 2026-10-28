package purchase

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5"
)

type store interface {
	TimeZone(context.Context, int64) (string, error)
	List(context.Context, int64, string) ([]Item, error)
	Create(context.Context, int64, string, Item) (Item, error)
	Delete(context.Context, int64, int64, string) (bool, error)
	Clear(context.Context, int64, string) error
}

type Service struct{ store store }

func NewService(store store) *Service { return &Service{store: store} }

func (s *Service) List(ctx context.Context, userID int64) ([]Item, float64, error) {
	month, err := s.nextMonth(ctx, userID)
	if err != nil {
		return nil, 0, err
	}
	items, err := s.store.List(ctx, userID, month)
	if err != nil {
		return nil, 0, fmt.Errorf("list purchases: %w", err)
	}
	var total float64
	for _, item := range items {
		total += item.Price
	}
	return items, total, nil
}

func (s *Service) Create(ctx context.Context, userID int64, name string, price float64, url *string) (Item, error) {
	item, err := NewItem(name, price, url)
	if err != nil {
		return Item{}, err
	}
	month, err := s.nextMonth(ctx, userID)
	if err != nil {
		return Item{}, err
	}
	saved, err := s.store.Create(ctx, userID, month, item)
	if err != nil {
		return Item{}, fmt.Errorf("create purchase: %w", err)
	}
	return saved, nil
}

func (s *Service) Delete(ctx context.Context, userID, id int64) (bool, error) {
	month, err := s.nextMonth(ctx, userID)
	if err != nil {
		return false, err
	}
	isDeleted, err := s.store.Delete(ctx, userID, id, month)
	if err != nil {
		return false, fmt.Errorf("delete purchase: %w", err)
	}
	return isDeleted, nil
}

func (s *Service) Clear(ctx context.Context, userID int64) error {
	month, err := s.nextMonth(ctx, userID)
	if err != nil {
		return err
	}
	if err := s.store.Clear(ctx, userID, month); err != nil {
		return fmt.Errorf("clear purchases: %w", err)
	}
	return nil
}

func (s *Service) nextMonth(ctx context.Context, userID int64) (string, error) {
	zone, err := s.store.TimeZone(ctx, userID)
	if errors.Is(err, pgx.ErrNoRows) {
		zone = "UTC"
	} else if err != nil {
		return "", fmt.Errorf("get purchase timezone: %w", err)
	}
	location, err := time.LoadLocation(zone)
	if err != nil {
		return "", fmt.Errorf("load purchase timezone: %w", err)
	}
	now := time.Now().In(location)
	return time.Date(now.Year(), now.Month()+1, 1, 0, 0, 0, 0, location).Format("2006-01-02"), nil
}
