package profile

import (
	"bytes"
	"context"
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"golang.org/x/crypto/bcrypt"

	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"github.com/jackc/pgx/v5/pgxpool"
)

func loginUser(t *testing.T, db *sql.DB) *http.Cookie {
	t.Helper()
	token := "profiletesttoken"
	hash := sha256.Sum256([]byte(token))
	hashStr := hex.EncodeToString(hash[:])
	expiresAt := time.Now().Add(24 * time.Hour)
	_, err := db.Exec(`
		INSERT INTO user_sessions (user_id, token_hash, expires_at)
		VALUES (1, $1, $2)
	`, hashStr, expiresAt)
	if err != nil {
		t.Fatalf("failed to insert session: %v", err)
	}
	return &http.Cookie{
		Name:  "life_session",
		Value: token,
	}
}

func TestShow(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()

	pool, poolErr := pgxpool.New(context.Background(), dsn)
	if poolErr != nil {
		t.Fatal(poolErr)
	}
	defer pool.Close()
	h := NewHandler(NewService(NewRepository(pool), auth.NewService(auth.NewRepository(pool))), func(r *http.Request) (int64, error) {
		user, err := testhelper.LookupSessionUser(db, r)
		return user.ID, err
	})
	cookie := loginUser(t, db)

	// 1. Show when the user doesn't have a profile yet
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodGet, "/profile", nil)
		req.AddCookie(cookie)
		h.Show(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected 200 OK, got %d", rec.Code)
		}

		var out map[string]any
		_ = json.NewDecoder(rec.Body).Decode(&out)
		if out["has_profile"] != false {
			t.Errorf("expected has_profile to be false, got %v", out["has_profile"])
		}
	}

	// Seed profile
	_, err := db.Exec(`INSERT INTO user_profiles (user_id, display_name) VALUES (1, 'Abhishek')`)
	if err != nil {
		t.Fatalf("failed to seed profile: %v", err)
	}

	// 2. Show when the user has set up a profile
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodGet, "/profile", nil)
		req.AddCookie(cookie)
		h.Show(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected 200 OK, got %d", rec.Code)
		}

		var out map[string]any
		_ = json.NewDecoder(rec.Body).Decode(&out)
		if out["has_profile"] != true {
			t.Errorf("expected has_profile to be true, got %v", out["has_profile"])
		}
		if out["name"] != "Abhishek" {
			t.Errorf("expected name Abhishek, got %v", out["name"])
		}
	}
}

func TestSave(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()

	pool, poolErr := pgxpool.New(context.Background(), dsn)
	if poolErr != nil {
		t.Fatal(poolErr)
	}
	defer pool.Close()
	h := NewHandler(NewService(NewRepository(pool), auth.NewService(auth.NewRepository(pool))), func(r *http.Request) (int64, error) {
		user, err := testhelper.LookupSessionUser(db, r)
		return user.ID, err
	})
	cookie := loginUser(t, db)

	// 1. Save profile - invalid JSON
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodPut, "/profile", bytes.NewReader([]byte("{invalid")))
		req.AddCookie(cookie)
		h.Save(rec, req)

		if rec.Code != http.StatusBadRequest {
			t.Errorf("expected 400 Bad Request, got %d", rec.Code)
		}
	}

	// 2. Save profile - empty name
	{
		rec := httptest.NewRecorder()
		body, _ := json.Marshal(saveBody{Name: ""})
		req := httptest.NewRequest(http.MethodPut, "/profile", bytes.NewReader(body))
		req.AddCookie(cookie)
		h.Save(rec, req)

		if rec.Code != http.StatusBadRequest {
			t.Errorf("expected 400 Bad Request, got %d", rec.Code)
		}
	}

	// 3. Save profile - successful create
	{
		rec := httptest.NewRecorder()
		body, _ := json.Marshal(saveBody{Name: "New Name"})
		req := httptest.NewRequest(http.MethodPut, "/profile", bytes.NewReader(body))
		req.AddCookie(cookie)
		h.Save(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected 200 OK, got %d", rec.Code)
		}

		var out map[string]string
		_ = json.NewDecoder(rec.Body).Decode(&out)
		if out["name"] != "New Name" {
			t.Errorf("expected name 'New Name', got %q", out["name"])
		}

		// Verify DB entry
		var name string
		err := db.QueryRow(`SELECT display_name FROM user_profiles WHERE user_id = 1`).Scan(&name)
		if err != nil {
			t.Fatalf("failed to query profile: %v", err)
		}
		if name != "New Name" {
			t.Errorf("expected DB profile name 'New Name', got %q", name)
		}
	}

	// 4. Save profile - successful update (on conflict)
	{
		rec := httptest.NewRecorder()
		body, _ := json.Marshal(saveBody{Name: "Updated Name"})
		req := httptest.NewRequest(http.MethodPut, "/profile", bytes.NewReader(body))
		req.AddCookie(cookie)
		h.Save(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected 200 OK, got %d", rec.Code)
		}

		var name string
		_ = db.QueryRow(`SELECT display_name FROM user_profiles WHERE user_id = 1`).Scan(&name)
		if name != "Updated Name" {
			t.Errorf("expected updated DB profile name 'Updated Name', got %q", name)
		}
	}
}

func TestChangePassword(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()

	pool, poolErr := pgxpool.New(context.Background(), dsn)
	if poolErr != nil {
		t.Fatal(poolErr)
	}
	defer pool.Close()
	h := NewHandler(NewService(NewRepository(pool), auth.NewService(auth.NewRepository(pool))), func(r *http.Request) (int64, error) {
		user, err := testhelper.LookupSessionUser(db, r)
		return user.ID, err
	})
	cookie := loginUser(t, db)

	// Seed user in database
	oldHash, err := bcrypt.GenerateFromPassword([]byte("oldpassword123"), bcrypt.DefaultCost)
	if err != nil {
		t.Fatalf("failed to hash password: %v", err)
	}
	_, err = db.Exec(`
		INSERT INTO users (id, username, password_hash)
		VALUES (1, 'testuser', $1)
		ON CONFLICT (id) DO UPDATE SET password_hash = EXCLUDED.password_hash
	`, string(oldHash))
	if err != nil {
		t.Fatalf("failed to seed user: %v", err)
	}

	// 1. Incorrect current password
	{
		rec := httptest.NewRecorder()
		body, _ := json.Marshal(changePasswordBody{
			CurrentPassword: "wrongpassword",
			NewPassword:     "newpassword123",
		})
		req := httptest.NewRequest(http.MethodPut, "/profile/password", bytes.NewReader(body))
		req.AddCookie(cookie)
		h.ChangePassword(rec, req)

		if rec.Code != http.StatusBadRequest {
			t.Errorf("expected 400 Bad Request for wrong current password, got %d", rec.Code)
		}
	}

	// 2. Short new password
	{
		rec := httptest.NewRecorder()
		body, _ := json.Marshal(changePasswordBody{
			CurrentPassword: "oldpassword123",
			NewPassword:     "123",
		})
		req := httptest.NewRequest(http.MethodPut, "/profile/password", bytes.NewReader(body))
		req.AddCookie(cookie)
		h.ChangePassword(rec, req)

		if rec.Code != http.StatusBadRequest {
			t.Errorf("expected 400 Bad Request for short new password, got %d", rec.Code)
		}
	}

	// 3. Successful password change
	{
		rec := httptest.NewRecorder()
		body, _ := json.Marshal(changePasswordBody{
			CurrentPassword: "oldpassword123",
			NewPassword:     "newpassword123",
		})
		req := httptest.NewRequest(http.MethodPut, "/profile/password", bytes.NewReader(body))
		req.AddCookie(cookie)
		h.ChangePassword(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected 200 OK, got %d", rec.Code)
		}

		var updatedHash string
		err := db.QueryRow(`SELECT password_hash FROM users WHERE id = 1`).Scan(&updatedHash)
		if err != nil {
			t.Fatalf("failed to query user password: %v", err)
		}

		if err := bcrypt.CompareHashAndPassword([]byte(updatedHash), []byte("newpassword123")); err != nil {
			t.Errorf("expected updated password hash to match new password: %v", err)
		}
	}
}
