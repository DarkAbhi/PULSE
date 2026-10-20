package purchase

import (
	"context"
	"errors"
	"testing"
)

type stubStore struct {
	items     []Item
	isDeleted bool
}

func (s *stubStore) List(context.Context, int64, string) ([]Item, error) { return s.items, nil }
func (s *stubStore) Create(_ context.Context, _ int64, _ string, item Item) (Item, error) {
	item.ID = 1
	s.items = append(s.items, item)
	return item, nil
}
func (s *stubStore) Delete(context.Context, int64, int64, string) (bool, error) {
	return s.isDeleted, nil
}
func (s *stubStore) Clear(context.Context, int64, string) error { s.items = nil; return nil }

func TestService(t *testing.T) {
	store := &stubStore{}
	service := NewService(store)
	if _, err := service.Create(context.Background(), 1, "", 1, nil); !errors.Is(err, ErrInvalidItem) {
		t.Fatalf("invalid input: %v", err)
	}
	if _, err := service.Create(context.Background(), 1, "Book", 12.5, nil); err != nil {
		t.Fatal(err)
	}
	items, total, err := service.List(context.Background(), 1)
	if err != nil || len(items) != 1 || total != 12.5 {
		t.Fatalf("list: %v, %v, %v", items, total, err)
	}
	if err := service.Clear(context.Background(), 1); err != nil || len(store.items) != 0 {
		t.Fatalf("clear: %v", err)
	}
}
