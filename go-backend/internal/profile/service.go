package profile

import (
	"context"
	"errors"
	"fmt"
	"strings"
	"time"
	_ "time/tzdata"

	"github.com/DarkAbhi/life-backend/internal/currency"
	"github.com/jackc/pgx/v5"
)

var ErrInvalidTimeZone = errors.New("profile: invalid IANA time zone")

var ErrInvalidName = errors.New("profile: name must be between 1 and 120 characters")
var ErrCurrentPasswordRequired = errors.New("profile: current password is required")
var ErrNewPasswordTooShort = errors.New("profile: new password must be at least 6 characters")

type store interface {
	Currency(context.Context, int64) (string, error)
	SaveCurrency(context.Context, int64, string) error
	Name(context.Context, int64) (string, error)
	TimeZone(context.Context, int64) (string, error)
	SaveTimeZone(context.Context, int64, string) error
	Save(context.Context, int64, string) error
	CreateAPIKey(context.Context, int64, string, string, string) (APIKey, error)
	ListAPIKeys(context.Context, int64) ([]APIKey, error)
	RevokeAPIKey(context.Context, int64, int64) (bool, error)
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
	return name, name != "", nil
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

// TimeZone returns the saved preference, defaulting to UTC before profile setup.
func (s *Service) TimeZone(ctx context.Context, userID int64) (string, error) {
	zone, err := s.store.TimeZone(ctx, userID)
	if errors.Is(err, pgx.ErrNoRows) {
		return "UTC", nil
	}
	if err != nil {
		return "", fmt.Errorf("get profile timezone: %w", err)
	}
	return zone, nil
}

func (s *Service) SaveTimeZone(ctx context.Context, userID int64, zone string) error {
	if !validTimeZone(zone) {
		return ErrInvalidTimeZone
	}
	if err := s.store.SaveTimeZone(ctx, userID, zone); err != nil {
		return fmt.Errorf("save profile timezone: %w", err)
	}
	return nil
}

func validTimeZone(zone string) bool {
	if zone == "" || zone == "Local" || len(zone) > 100 {
		return false
	}
	_, err := time.LoadLocation(zone)
	return err == nil
}

var ErrInvalidCurrency = errors.New("profile: unsupported currency")

func (s *Service) Currency(ctx context.Context, userID int64) (string, error) {
	code, err := s.store.Currency(ctx, userID)
	if errors.Is(err, pgx.ErrNoRows) {
		return currency.Default, nil
	}
	if err != nil {
		return "", fmt.Errorf("get profile currency: %w", err)
	}
	return code, nil
}
func (s *Service) SaveCurrency(ctx context.Context, userID int64, code string) error {
	if !currency.Valid(code) {
		return ErrInvalidCurrency
	}
	if err := s.store.SaveCurrency(ctx, userID, code); err != nil {
		return fmt.Errorf("save profile currency: %w", err)
	}
	return nil
}
