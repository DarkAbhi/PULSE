package profile

import (
	"context"
	"errors"
	"fmt"
	"strings"

	"github.com/jackc/pgx/v5"
)

var ErrInvalidName = errors.New("profile: name must be between 1 and 120 characters")
var ErrCurrentPasswordRequired = errors.New("profile: current password is required")
var ErrNewPasswordTooShort = errors.New("profile: new password must be at least 6 characters")

type store interface {
	Name(context.Context, int64) (string, error)
	Save(context.Context, int64, string) error
}
type passwordChanger interface {
	ChangePassword(context.Context, int64, string, string) error
}
type Service struct {
	store     store
	passwords passwordChanger
}

func NewService(store store, passwords passwordChanger) *Service {
	return &Service{store: store, passwords: passwords}
}
func (s *Service) Fetch(ctx context.Context, userID int64) (string, bool, error) {
	name, err := s.store.Name(ctx, userID)
	if errors.Is(err, pgx.ErrNoRows) {
		return "", false, nil
	}
	if err != nil {
		return "", false, fmt.Errorf("get profile: %w", err)
	}
	return name, true, nil
}
func (s *Service) Save(ctx context.Context, userID int64, name string) (string, error) {
	name = strings.TrimSpace(name)
	if name == "" || len([]rune(name)) > 120 {
		return "", ErrInvalidName
	}
	if err := s.store.Save(ctx, userID, name); err != nil {
		return "", fmt.Errorf("save profile: %w", err)
	}
	return name, nil
}
func (s *Service) ChangePassword(ctx context.Context, userID int64, current, next string) error {
	if current == "" {
		return ErrCurrentPasswordRequired
	}
	if len(next) < 6 {
		return ErrNewPasswordTooShort
	}
	return s.passwords.ChangePassword(ctx, userID, current, next)
}
