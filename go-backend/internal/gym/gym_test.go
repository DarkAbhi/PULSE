package gym

import (
	"bytes"
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
	"github.com/DarkAbhi/life-backend/internal/notification"
	"github.com/go-chi/chi/v5"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"github.com/DarkAbhi/life-backend/internal/timeutil"
)

func loginUser(t *testing.T, db *sql.DB) *http.Cookie {
	t.Helper()
	token := "gymtesttoken"
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

func TestGymVisitedToday(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()

	h := newTestGymHandler(t, db, dsn)

	// 1. Check visited when not visited
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodGet, "/workout/today", nil)
		h.GymVisitedToday(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected 200, got %d", rec.Code)
		}

		var out map[string]any
		_ = json.NewDecoder(rec.Body).Decode(&out)
		if out["visited"] != false {
			t.Errorf("expected visited to be false, got %v", out["visited"])
		}
	}

	// 2. Add workout and check visited today
	{
		recAdd := httptest.NewRecorder()
		reqAdd := httptest.NewRequest(http.MethodPost, "/workout/today", nil)
		h.AddWorkoutForDay(recAdd, reqAdd)
		if recAdd.Code != http.StatusCreated {
			t.Fatalf("failed to add workout: %d", recAdd.Code)
		}

		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodGet, "/workout/today", nil)
		h.GymVisitedToday(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected 200, got %d", rec.Code)
		}

		var out map[string]any
		_ = json.NewDecoder(rec.Body).Decode(&out)
		if out["visited"] != true {
			t.Errorf("expected visited to be true, got %v", out["visited"])
		}
		if out["id"] == nil {
			t.Error("expected visit ID in response")
		}
	}
}

func TestListGymVisitsAndDelete(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()

	h := newTestGymHandler(t, db, dsn)

	// Seed gym visits
	_, err := db.Exec(`
		INSERT INTO gym_visits (created_at) VALUES 
		(NOW() - INTERVAL '1 day'),
		(NOW())
	`)
	if err != nil {
		t.Fatalf("failed to seed: %v", err)
	}

	// Get latest ID to delete later
	var latestID int64
	err = db.QueryRow(`SELECT id FROM gym_visits ORDER BY created_at DESC LIMIT 1`).Scan(&latestID)
	if err != nil {
		t.Fatalf("failed to get latest: %v", err)
	}

	// 1. List visits
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodGet, "/gym-visits", nil)
		h.ListGymVisits(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected 200 OK, got %d", rec.Code)
		}

		var out []gymVisitListItem
		_ = json.NewDecoder(rec.Body).Decode(&out)
		if len(out) != 2 {
			t.Errorf("expected 2 visits, got %d", len(out))
		}
	}

	// 2. Delete visit
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodDelete, "/gym-visits/{id}", nil)

		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(latestID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.DeleteGymVisit(rec, req)

		if rec.Code != http.StatusNoContent {
			t.Errorf("expected 204 No Content, got %d", rec.Code)
		}

		// Verify deletion
		var count int
		_ = db.QueryRow(`SELECT COUNT(*) FROM gym_visits WHERE id = $1`, latestID).Scan(&count)
		if count != 0 {
			t.Errorf("expected visit to be deleted, got count %d", count)
		}
	}
}

func TestExercisesManagement(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()

	h := newTestGymHandler(t, db, dsn)

	// Seed visit
	var visitID int64
	err := db.QueryRow(`INSERT INTO gym_visits DEFAULT VALUES RETURNING id`).Scan(&visitID)
	if err != nil {
		t.Fatalf("failed to seed visit: %v", err)
	}

	// 1. Get exercises (empty initially)
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodGet, "/gym-visits/{id}/exercises", nil)
		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(visitID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.GetGymVisitExercises(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected 200, got %d", rec.Code)
		}

		var out []gymExerciseDTO
		_ = json.NewDecoder(rec.Body).Decode(&out)
		if len(out) != 0 {
			t.Errorf("expected 0 exercises, got %d", len(out))
		}
	}

	// 2. Create Exercise - Input validation error (reps = 0)
	{
		rec := httptest.NewRecorder()
		body, _ := json.Marshal(createGymExerciseBody{
			Name: "Bench Press",
			Sets: []exerciseSetInput{{Reps: 0, Weight: nil}},
		})
		req := httptest.NewRequest(http.MethodPost, "/gym-visits/{id}/exercises", bytes.NewReader(body))
		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(visitID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.CreateGymVisitExercise(rec, req)
		if rec.Code != http.StatusBadRequest {
			t.Errorf("expected 400 Bad Request, got %d", rec.Code)
		}
	}

	// 3. Create Exercise - Success
	{
		rec := httptest.NewRecorder()
		weightVal := 60.5
		body, _ := json.Marshal(createGymExerciseBody{
			Name: "Bench Press",
			Sets: []exerciseSetInput{{Reps: 10, Weight: &weightVal}},
		})
		req := httptest.NewRequest(http.MethodPost, "/gym-visits/{id}/exercises", bytes.NewReader(body))
		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(visitID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.CreateGymVisitExercise(rec, req)

		if rec.Code != http.StatusCreated {
			t.Errorf("expected 201 Created, got %d", rec.Code)
		}

		var out gymExerciseDTO
		_ = json.NewDecoder(rec.Body).Decode(&out)
		if out.Name != "Bench Press" || len(out.Sets) != 1 || *out.Sets[0].Weight != weightVal || out.Sets[0].Reps != 10 {
			t.Errorf("unexpected saved exercise details: %+v", out)
		}
	}
}

func TestMarkGymReminderVisited(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()

	h := newTestGymHandler(t, db, dsn)
	cookie := loginUser(t, db)

	// Seed gym reminder notification
	var notificationID int64
	err := db.QueryRow(`
		INSERT INTO notifications (user_id, source, title, body, target_path, priority, metadata)
		VALUES (1, 'Gym reminder', 'Time for the gym', 'Gym reminder', '/gym-visits', 1, '{}')
		RETURNING id
	`).Scan(&notificationID)
	if err != nil {
		t.Fatalf("failed to seed: %v", err)
	}

	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodPost, "/notifications/{id}/gym-visit", nil)
	req.AddCookie(cookie)

	rctx := chi.NewRouteContext()
	rctx.URLParams.Add("id", strconv.FormatInt(notificationID, 10))
	req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

	h.MarkGymReminderVisited(rec, req)

	if rec.Code != http.StatusCreated {
		t.Errorf("expected 201 Created, got %d", rec.Code)
	}

	// Verify visit created
	var count int
	_ = db.QueryRow(`SELECT COUNT(*) FROM gym_visits`).Scan(&count)
	if count != 1 {
		t.Errorf("expected 1 gym visit, got %d", count)
	}

	// Verify notification dismissed
	var dismissed sql.NullTime
	_ = db.QueryRow(`SELECT dismissed_at FROM notifications WHERE id = $1`, notificationID).Scan(&dismissed)
	if !dismissed.Valid {
		t.Error("expected notification dismissed_at to be populated")
	}
}

func TestCreateDueGymReminders(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()

	// Use Wednesday at 4:00 PM for the test run time (a weekday past 3:30 PM)
	loc, _ := time.LoadLocation(timeutil.IndiaTimeZone)
	testTime := time.Date(2026, 7, 15, 16, 0, 0, 0, loc).UTC()

	// 1. Run reminder creation
	if err := newTestGymService(t, dsn).CreateDueReminders(context.Background(), testTime); err != nil {
		t.Fatal(err)
	}

	// Verify notification created for user 1
	var notifCount int
	err := db.QueryRow(`
		SELECT COUNT(*) FROM notifications 
		WHERE user_id = 1 AND source = 'Gym reminder'
	`).Scan(&notifCount)
	if err != nil {
		t.Fatalf("failed to query: %v", err)
	}
	if notifCount != 1 {
		t.Errorf("expected 1 notification, got %d", notifCount)
	}

	// Verify reminder delivery entry exists
	var deliveryCount int
	err = db.QueryRow(`
		SELECT COUNT(*) FROM gym_reminder_deliveries 
		WHERE user_id = 1 AND reminder_date = '2026-07-15'
	`).Scan(&deliveryCount)
	if err != nil {
		t.Fatalf("failed to query: %v", err)
	}
	if deliveryCount != 1 {
		t.Errorf("expected 1 delivery record, got %d", deliveryCount)
	}

	// 2. Running a second time on the same day should not create duplicates
	if err := newTestGymService(t, dsn).CreateDueReminders(context.Background(), testTime.Add(5*time.Minute)); err != nil {
		t.Fatal(err)
	}
	_ = db.QueryRow(`SELECT COUNT(*) FROM notifications WHERE user_id = 1 AND source = 'Gym reminder'`).Scan(&notifCount)
	if notifCount != 1 {
		t.Errorf("expected still 1 notification, got %d", notifCount)
	}
}

func newTestGymService(t *testing.T, dsn string) *Service {
	t.Helper()
	pool, err := pgxpool.New(context.Background(), dsn)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(pool.Close)
	notifications := notification.NewService(notification.NewRepository(pool))
	return NewService(NewRepository(pool), notifications, auth.NewService(auth.NewRepository(pool)))
}

func newTestGymHandler(t *testing.T, db *sql.DB, dsn string) *Handler {
	service := newTestGymService(t, dsn)
	return NewHandler(service, func(r *http.Request) (int64, error) { user, err := auth.GetSessionUser(db, r); return user.ID, err })
}
