package auth

import (
	"bytes"
	"context"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"golang.org/x/crypto/bcrypt"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"github.com/jackc/pgx/v5/pgxpool"
)

type failingLogoutStore struct{ store }

func (failingLogoutStore) DeleteSession(context.Context, string) error {
	return errors.New("database unavailable")
}

func TestLogoutFailureKeepsCookie(t *testing.T) {
	h := NewHandler(NewService(failingLogoutStore{}), false)
	req := httptest.NewRequest(http.MethodPost, "/auth/logout", nil)
	req.AddCookie(&http.Cookie{Name: SessionCookieName, Value: "session-token"})
	rec := httptest.NewRecorder()
	h.Logout(rec, req)
	if rec.Code != http.StatusInternalServerError {
		t.Fatalf("expected 500, got %d", rec.Code)
	}
	if len(rec.Result().Cookies()) != 0 {
		t.Fatal("logout failure cleared the session cookie")
	}
}

func TestBootstrapPasswordHash(t *testing.T) {
	const bootstrapHash = "$2y$12$bB7WwVq7nGJ4cfNTCX6kQODcNRLQvMjRhIFuH4Qv2.GAxlqNac4/S"
	if err := bcrypt.CompareHashAndPassword([]byte(bootstrapHash), []byte("password")); err != nil {
		t.Fatalf("bootstrap password hash does not match the documented password: %v", err)
	}
}

func TestLoginAndSession(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()

	pool, poolErr := pgxpool.New(context.Background(), dsn)
	if poolErr != nil {
		t.Fatal(poolErr)
	}
	defer pool.Close()
	h := NewHandler(NewService(NewRepository(pool)), false)

	// 1. Test Login - Missing fields
	{
		rec := httptest.NewRecorder()
		body, _ := json.Marshal(loginBody{Username: "", Password: ""})
		req := httptest.NewRequest(http.MethodPost, "/auth/login", bytes.NewReader(body))
		h.Login(rec, req)
		if rec.Code != http.StatusBadRequest {
			t.Errorf("expected 400 Bad Request, got %d", rec.Code)
		}
	}

	// 2. Test Login - Invalid credentials
	{
		rec := httptest.NewRecorder()
		body, _ := json.Marshal(loginBody{Username: "admin", Password: "wrongpassword"})
		req := httptest.NewRequest(http.MethodPost, "/auth/login", bytes.NewReader(body))
		h.Login(rec, req)
		if rec.Code != http.StatusUnauthorized {
			t.Errorf("expected 401 Unauthorized, got %d", rec.Code)
		}
	}

	// 3. Test Login - Success
	var sessionCookie *http.Cookie
	{
		rec := httptest.NewRecorder()
		body, _ := json.Marshal(loginBody{Username: "admin", Password: "password"})
		req := httptest.NewRequest(http.MethodPost, "/auth/login", bytes.NewReader(body))
		h.Login(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected 200 OK, got %d", rec.Code)
		}

		cookies := rec.Result().Cookies()
		for _, cookie := range cookies {
			if cookie.Name == SessionCookieName {
				sessionCookie = cookie
				break
			}
		}
		if sessionCookie == nil {
			t.Fatal("expected session cookie in response, got none")
		}
	}

	// 4. Test Session - Unauthorized
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodGet, "/auth/session", nil)
		h.Session(rec, req)
		if rec.Code != http.StatusUnauthorized {
			t.Errorf("expected 401 Unauthorized for session, got %d", rec.Code)
		}
	}

	// 5. Test Session - Success (with cookie)
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodGet, "/auth/session", nil)
		req.AddCookie(sessionCookie)
		h.Session(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected 200 OK for session, got %d", rec.Code)
		}

		var out map[string]string
		_ = json.NewDecoder(rec.Body).Decode(&out)
		if out["username"] != "admin" {
			t.Errorf("expected username admin, got %q", out["username"])
		}
	}

	// 6. Test LookupSessionUser directly (with cookie)
	{
		req := httptest.NewRequest(http.MethodGet, "/auth/session", nil)
		req.AddCookie(sessionCookie)
		user, err := testhelper.LookupSessionUser(db, req)
		if err != nil {
			t.Fatalf("expected LookupSessionUser to succeed with cookie, got %v", err)
		}
		if user.Username != "admin" {
			t.Errorf("expected username admin, got %q", user.Username)
		}
		if user.ID <= 0 {
			t.Errorf("expected positive user ID, got %d", user.ID)
		}
	}

	// 7. Test LookupSessionUser directly (with Authorization: Bearer header)
	{
		req := httptest.NewRequest(http.MethodGet, "/auth/session", nil)
		req.Header.Set("Authorization", "Bearer "+sessionCookie.Value)
		user, err := testhelper.LookupSessionUser(db, req)
		if err != nil {
			t.Fatalf("expected LookupSessionUser to succeed with Bearer header, got %v", err)
		}
		if user.Username != "admin" {
			t.Errorf("expected username admin, got %q", user.Username)
		}
	}
}

func TestSessionUserExpired(t *testing.T) {
	db, _, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()

	// Insert an expired session manually
	token := "expiredtokenvalue12345"
	hash := sha256.Sum256([]byte(token))
	hashStr := hex.EncodeToString(hash[:])
	expiredAt := time.Now().Add(-1 * time.Hour)

	_, err := db.Exec(`
		INSERT INTO user_sessions (user_id, token_hash, expires_at)
		VALUES (1, $1, $2)
	`, hashStr, expiredAt)
	if err != nil {
		t.Fatalf("failed to insert expired session: %v", err)
	}

	req := httptest.NewRequest(http.MethodGet, "/", nil)
	req.AddCookie(&http.Cookie{
		Name:  SessionCookieName,
		Value: token,
	})

	_, err = testhelper.LookupSessionUser(db, req)
	if err == nil {
		t.Fatal("expected LookupSessionUser to fail for expired token, but got nil error")
	}
}
