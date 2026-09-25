package notification

import (
	"context"
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strconv"
	"testing"
	"time"

	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/go-chi/chi/v5"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
)

func loginUser(t *testing.T, db *sql.DB) *http.Cookie {
	t.Helper()
	token := "notiftesttoken"
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

func TestListNotifications(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()

	pool, poolErr := pgxpool.New(context.Background(), dsn)
	if poolErr != nil {
		t.Fatal(poolErr)
	}
	defer pool.Close()
	h := NewHandler(NewService(NewRepository(pool)), func(r *http.Request) (int64, error) { user, err := auth.GetSessionUser(db, r); return user.ID, err })
	cookie := loginUser(t, db)

	// Seed notifications
	_, err := db.Exec(`
		INSERT INTO notifications (user_id, source, title, body, target_path, priority)
		VALUES 
		(1, 'Garage', 'Air Fill Check', 'Check tires', '/garage', 1),
		(1, 'Gym reminder', 'Time for workout', 'Go to gym', '/gym-visits', 1),
		(1, 'System', 'Dismissed reminder', 'Should not show', '/home', 0)
	`)
	if err != nil {
		t.Fatalf("failed to seed notifications: %v", err)
	}

	// Update the third one to be dismissed
	_, err = db.Exec(`UPDATE notifications SET dismissed_at = NOW() WHERE title = 'Dismissed reminder'`)
	if err != nil {
		t.Fatalf("failed to dismiss notification: %v", err)
	}

	// 1. List active notifications
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodGet, "/notifications", nil)
		req.AddCookie(cookie)
		h.ListNotifications(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected 200 OK, got %d", rec.Code)
		}

		var out []notificationDTO
		_ = json.NewDecoder(rec.Body).Decode(&out)
		if len(out) != 2 {
			t.Errorf("expected 2 active notifications, got %d", len(out))
		}

		// Verify order (newest first, which is the last inserted)
		if out[0].Title != "Time for workout" {
			t.Errorf("expected newest first ('Time for workout'), got %q", out[0].Title)
		}
	}

	// 2. List notifications with limit query param
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodGet, "/notifications?limit=1", nil)
		req.AddCookie(cookie)
		h.ListNotifications(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected 200 OK, got %d", rec.Code)
		}

		var out []notificationDTO
		_ = json.NewDecoder(rec.Body).Decode(&out)
		if len(out) != 1 {
			t.Errorf("expected 1 notification with limit=1, got %d", len(out))
		}
	}
}

func TestDismissNotification(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()

	pool, poolErr := pgxpool.New(context.Background(), dsn)
	if poolErr != nil {
		t.Fatal(poolErr)
	}
	defer pool.Close()
	h := NewHandler(NewService(NewRepository(pool)), func(r *http.Request) (int64, error) { user, err := auth.GetSessionUser(db, r); return user.ID, err })
	cookie := loginUser(t, db)

	// Seed notification
	var notificationID int64
	err := db.QueryRow(`
		INSERT INTO notifications (user_id, source, title, body, target_path, priority)
		VALUES (1, 'Garage', 'Air Fill Check', 'Check tires', '/garage', 1)
		RETURNING id
	`).Scan(&notificationID)
	if err != nil {
		t.Fatalf("failed to seed notification: %v", err)
	}

	// Dismiss notification
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodDelete, "/notifications/{id}", nil)
	req.AddCookie(cookie)

	// Setup chi context
	rctx := chi.NewRouteContext()
	rctx.URLParams.Add("id", strconvFormat(notificationID))
	req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

	// Direct parsing of id in parseID uses chi.URLParam(r, "id")
	// Let's call the handler
	h.DismissNotification(rec, req)

	if rec.Code != http.StatusNoContent {
		t.Errorf("expected 204 No Content, got %d", rec.Code)
	}

	// Verify DB state
	var dismissedAt sql.NullTime
	err = db.QueryRow(`SELECT dismissed_at FROM notifications WHERE id = $1`, notificationID).Scan(&dismissedAt)
	if err != nil {
		t.Fatalf("failed to query notification: %v", err)
	}
	if !dismissedAt.Valid {
		t.Error("expected dismissed_at to be valid (not null)")
	}
}

func TestClearNotifications(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()

	pool, poolErr := pgxpool.New(context.Background(), dsn)
	if poolErr != nil {
		t.Fatal(poolErr)
	}
	defer pool.Close()
	h := NewHandler(NewService(NewRepository(pool)), func(r *http.Request) (int64, error) { user, err := auth.GetSessionUser(db, r); return user.ID, err })
	cookie := loginUser(t, db)

	// Seed notifications
	_, err := db.Exec(`
		INSERT INTO notifications (user_id, source, title, body, target_path, priority)
		VALUES 
		(1, 'Garage', 'Air Check 1', 'Check tires', '/garage', 1),
		(1, 'Garage', 'Air Check 2', 'Check tires', '/garage', 1)
	`)
	if err != nil {
		t.Fatalf("failed to seed: %v", err)
	}

	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodDelete, "/notifications", nil)
	req.AddCookie(cookie)
	h.ClearNotifications(rec, req)

	if rec.Code != http.StatusNoContent {
		t.Errorf("expected 204 No Content, got %d", rec.Code)
	}

	// Verify all dismissed
	var count int
	err = db.QueryRow(`SELECT COUNT(*) FROM notifications WHERE user_id = 1 AND dismissed_at IS NULL`).Scan(&count)
	if err != nil {
		t.Fatalf("failed to count: %v", err)
	}
	if count != 0 {
		t.Errorf("expected 0 active notifications after clear, got %d", count)
	}
}

func strconvFormat(i int64) string {
	return strconv.FormatInt(i, 10)
}
