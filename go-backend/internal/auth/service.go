package auth

import (
	"context"
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/jackc/pgx/v5"
	"golang.org/x/crypto/bcrypt"
)

var ErrInvalidCredentials = errors.New("auth: invalid username or password")
var ErrSessionNotFound = errors.New("auth: session is invalid or expired")
var ErrUserNotFound = errors.New("auth: user not found")
var ErrWrongPassword = errors.New("auth: current password is incorrect")

type store interface {
	LoginUser(context.Context, string) (int64, string, error)
	CreateSession(context.Context, int64, string, time.Time) error
	DeleteSession(context.Context, string) error
	SessionUser(context.Context, string) (SessionUser, error)
	PasswordHash(context.Context, int64) (string, error)
	UpdatePasswordHash(context.Context, int64, string) error
	ListUserIDs(context.Context) ([]int64, error)
}
type Service struct{ store store }

func NewService(store store) *Service { return &Service{store: store} }

func (s *Service) Login(ctx context.Context, username, password string) (string, time.Time, error) {
	username = strings.TrimSpace(username)
	if username == "" || password == "" {
		return "", time.Time{}, ErrInvalidCredentials
	}
	id, hash, err := s.store.LoginUser(ctx, username)
	if errors.Is(err, pgx.ErrNoRows) {
		return "", time.Time{}, ErrInvalidCredentials
	}
	if err != nil {
		return "", time.Time{}, fmt.Errorf("get login user: %w", err)
	}
	if bcrypt.CompareHashAndPassword([]byte(hash), []byte(password)) != nil {
		return "", time.Time{}, ErrInvalidCredentials
	}
	tokenBytes := make([]byte, 32)
	if _, err := rand.Read(tokenBytes); err != nil {
		return "", time.Time{}, fmt.Errorf("generate session token: %w", err)
	}
	token := hex.EncodeToString(tokenBytes)
	expires := time.Now().UTC().Add(SessionLifetime)
	if err := s.store.CreateSession(ctx, id, tokenHash(token), expires); err != nil {
		return "", time.Time{}, fmt.Errorf("create session: %w", err)
	}
	return token, expires, nil
}
func (s *Service) SessionUser(ctx context.Context, token string) (SessionUser, error) {
	if token == "" {
		return SessionUser{}, ErrSessionNotFound
	}
	user, err := s.store.SessionUser(ctx, tokenHash(token))
	if errors.Is(err, pgx.ErrNoRows) {
		return SessionUser{}, ErrSessionNotFound
	}
	if err != nil {
		return SessionUser{}, fmt.Errorf("get session user: %w", err)
	}
	return user, nil
}
func (s *Service) Logout(ctx context.Context, token string) error {
	if token == "" {
		return nil
	}
	if err := s.store.DeleteSession(ctx, tokenHash(token)); err != nil {
		return fmt.Errorf("delete session: %w", err)
	}
	return nil
}
func (s *Service) ChangePassword(ctx context.Context, userID int64, current, next string) error {
	hash, err := s.store.PasswordHash(ctx, userID)
	if errors.Is(err, pgx.ErrNoRows) {
		return ErrUserNotFound
	}
	if err != nil {
		return fmt.Errorf("get password hash: %w", err)
	}
	if bcrypt.CompareHashAndPassword([]byte(hash), []byte(current)) != nil {
		return ErrWrongPassword
	}
	newHash, err := bcrypt.GenerateFromPassword([]byte(next), bcrypt.DefaultCost)
	if err != nil {
		return fmt.Errorf("hash password: %w", err)
	}
	if err := s.store.UpdatePasswordHash(ctx, userID, string(newHash)); err != nil {
		return fmt.Errorf("update password hash: %w", err)
	}
	return nil
}
func (s *Service) ListUserIDs(ctx context.Context) ([]int64, error) {
	ids, err := s.store.ListUserIDs(ctx)
	if err != nil {
		return nil, fmt.Errorf("list users: %w", err)
	}
	return ids, nil
}
func tokenHash(token string) string {
	hash := sha256.Sum256([]byte(token))
	return hex.EncodeToString(hash[:])
}
