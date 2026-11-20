package profile

import (
	"context"
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"fmt"
	"strings"
	"time"
)

var ErrInvalidAPIKeyName = errors.New("profile: API key name must be between 1 and 120 characters")

type APIKey struct {
	ID          int64     `json:"id"`
	Name        string    `json:"name"`
	TokenPrefix string    `json:"token_prefix"`
	CreatedAt   time.Time `json:"created_at"`
}

func (s *Service) CreateAPIKey(ctx context.Context, userID int64, name string) (APIKey, string, error) {
	name = strings.TrimSpace(name)
	if name == "" || len([]rune(name)) > 120 {
		return APIKey{}, "", ErrInvalidAPIKeyName
	}
	random := make([]byte, 32)
	if _, err := rand.Read(random); err != nil {
		return APIKey{}, "", fmt.Errorf("generate API key: %w", err)
	}
	token := "lt_" + hex.EncodeToString(random)
	hash := sha256.Sum256([]byte(token))
	key, err := s.store.CreateAPIKey(ctx, userID, name, hex.EncodeToString(hash[:]), token[:11])
	if err != nil {
		return APIKey{}, "", fmt.Errorf("create API key: %w", err)
	}
	return key, token, nil
}

func (s *Service) ListAPIKeys(ctx context.Context, userID int64) ([]APIKey, error) {
	keys, err := s.store.ListAPIKeys(ctx, userID)
	if err != nil {
		return nil, fmt.Errorf("list API keys: %w", err)
	}
	return keys, nil
}

func (s *Service) RevokeAPIKey(ctx context.Context, userID, keyID int64) (bool, error) {
	revoked, err := s.store.RevokeAPIKey(ctx, userID, keyID)
	if err != nil {
		return false, fmt.Errorf("revoke API key: %w", err)
	}
	return revoked, nil
}
