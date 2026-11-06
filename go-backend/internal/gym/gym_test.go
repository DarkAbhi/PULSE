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

	"github.com/go-chi/chi/v5"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
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

func TestVisitedToday(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()

	h := newTestHandler(t, db, dsn)
	cookie := loginUser(t, db)

	// 1. Check visited when not visited
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodGet, "/workout/today", nil)
		req.AddCookie(cookie)
		h.VisitedToday(rec, req)

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
		reqAdd.AddCookie(cookie)
		h.AddWorkoutForDay(recAdd, reqAdd)
		if recAdd.Code != http.StatusCreated {
			t.Fatalf("failed to add workout: %d", recAdd.Code)
		}

		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodGet, "/workout/today", nil)
		req.AddCookie(cookie)
		h.VisitedToday(rec, req)

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

func TestListVisitsAndDelete(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()

	h := newTestHandler(t, db, dsn)
	cookie := loginUser(t, db)

	// Seed gym visits
	_, err := db.Exec(`
		INSERT INTO gym_visits (user_id, created_at) VALUES
		(1, NOW() - INTERVAL '1 day'),
		(1, NOW())
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
		req.AddCookie(cookie)
		h.ListVisits(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected 200 OK, got %d", rec.Code)
		}

		var out []visitListItem
		_ = json.NewDecoder(rec.Body).Decode(&out)
		if len(out) != 2 {
			t.Errorf("expected 2 visits, got %d", len(out))
		}
	}

	// 2. Delete visit
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodDelete, "/gym-visits/{id}", nil)
		req.AddCookie(cookie)

		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(latestID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.DeleteVisit(rec, req)

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

	h := newTestHandler(t, db, dsn)
	cookie := loginUser(t, db)

	// Seed visit
	var visitID int64
	err := db.QueryRow(`INSERT INTO gym_visits (user_id) VALUES (1) RETURNING id`).Scan(&visitID)
	if err != nil {
		t.Fatalf("failed to seed visit: %v", err)
	}

	// 1. Get exercises (empty initially)
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodGet, "/gym-visits/{id}/exercises", nil)
		req.AddCookie(cookie)
		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(visitID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.ListVisitExercises(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected 200, got %d", rec.Code)
		}

		var out []exerciseDTO
		_ = json.NewDecoder(rec.Body).Decode(&out)
		if len(out) != 0 {
			t.Errorf("expected 0 exercises, got %d", len(out))
		}
	}

	// 2. Create Exercise - Input validation error (reps = 0)
	{
		rec := httptest.NewRecorder()
		body, _ := json.Marshal(createExerciseBody{
			Name: "Bench Press",
			Sets: []exerciseSetInput{{Reps: 0, Weight: nil}},
		})
		req := httptest.NewRequest(http.MethodPost, "/gym-visits/{id}/exercises", bytes.NewReader(body))
		req.AddCookie(cookie)
		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(visitID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.CreateVisitExercise(rec, req)
		if rec.Code != http.StatusBadRequest {
			t.Errorf("expected 400 Bad Request, got %d", rec.Code)
		}
	}

	// 3. Create Exercise - Success
	{
		rec := httptest.NewRecorder()
		weightVal := 60.5
		body, _ := json.Marshal(createExerciseBody{
			Name: "Bench Press",
			Sets: []exerciseSetInput{{Reps: 10, Weight: &weightVal}},
		})
		req := httptest.NewRequest(http.MethodPost, "/gym-visits/{id}/exercises", bytes.NewReader(body))
		req.AddCookie(cookie)
		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(visitID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.CreateVisitExercise(rec, req)

		if rec.Code != http.StatusCreated {
			t.Errorf("expected 201 Created, got %d", rec.Code)
		}

		var out exerciseDTO
		_ = json.NewDecoder(rec.Body).Decode(&out)
		if out.Name != "Bench Press" || len(out.Sets) != 1 || *out.Sets[0].Weight != weightVal || out.Sets[0].Reps != 10 {
			t.Errorf("unexpected saved exercise details: %+v", out)
		}
	}
}

func TestCreateVisitExercisesBatch(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()
	h := newTestHandler(t, db, dsn)
	cookie := loginUser(t, db)
	var visitID int64
	if err := db.QueryRow(`INSERT INTO gym_visits (user_id) VALUES (1) RETURNING id`).Scan(&visitID); err != nil {
		t.Fatal(err)
	}
	post := func(exercises []createExerciseBody) *httptest.ResponseRecorder {
		t.Helper()
		body, err := json.Marshal(createExercisesBody{Exercises: exercises})
		if err != nil {
			t.Fatal(err)
		}
		req := httptest.NewRequest(http.MethodPost, "/gym-visits/{id}/exercises/batch", bytes.NewReader(body))
		req.AddCookie(cookie)
		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(visitID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))
		rec := httptest.NewRecorder()
		h.CreateVisitExercises(rec, req)
		return rec
	}
	weight := 12.5
	exercises := []createExerciseBody{
		{Name: "Bicep curls", Sets: []exerciseSetInput{{Reps: 10, Weight: &weight}, {Reps: 8, Weight: &weight}}},
		{Name: "Barbell squats", Sets: []exerciseSetInput{{Reps: 8, Weight: &weight}}},
	}
	invalid := append([]createExerciseBody(nil), exercises...)
	invalid[1].Sets = []exerciseSetInput{{Reps: 0, Weight: &weight}}
	if rec := post(invalid); rec.Code != http.StatusBadRequest {
		t.Fatalf("invalid batch: status %d, body %s", rec.Code, rec.Body.String())
	}
	var count int
	if err := db.QueryRow(`SELECT COUNT(*) FROM gym_visit_exercises WHERE gym_visit_id=$1`, visitID).Scan(&count); err != nil || count != 0 {
		t.Fatalf("invalid batch saved exercises: count=%d err=%v", count, err)
	}
	tooLarge := 1000000.0
	exercises[1].Sets[0].Weight = &tooLarge
	if rec := post(exercises); rec.Code != http.StatusInternalServerError {
		t.Fatalf("failed insert: status %d, body %s", rec.Code, rec.Body.String())
	}
	if err := db.QueryRow(`SELECT COUNT(*) FROM gym_visit_exercises WHERE gym_visit_id=$1`, visitID).Scan(&count); err != nil || count != 0 {
		t.Fatalf("failed batch left partial exercises: count=%d err=%v", count, err)
	}
	exercises[1].Sets[0].Weight = &weight
	rec := post(exercises)
	if rec.Code != http.StatusCreated {
		t.Fatalf("valid batch: status %d, body %s", rec.Code, rec.Body.String())
	}
	var saved []exerciseDTO
	if err := json.NewDecoder(rec.Body).Decode(&saved); err != nil {
		t.Fatal(err)
	}
	if len(saved) != 2 || saved[0].Name != "Bicep curls" || saved[1].Name != "Barbell squats" ||
		len(saved[0].Sets) != 2 || saved[0].Sets[0].SetNumber != 1 || saved[0].Sets[1].SetNumber != 2 ||
		*saved[0].Sets[0].Weight != weight || saved[1].Sets[0].Reps != 8 {
		t.Fatalf("unexpected saved batch: %+v", saved)
	}
	var otherUserID int64
	if err := db.QueryRow(`INSERT INTO users (username, password_hash) VALUES ('other-batch-user', 'test') RETURNING id`).Scan(&otherUserID); err != nil {
		t.Fatal(err)
	}
	if err := db.QueryRow(`INSERT INTO gym_visits (user_id) VALUES ($1) RETURNING id`, otherUserID).Scan(&visitID); err != nil {
		t.Fatal(err)
	}
	if rec := post(exercises); rec.Code != http.StatusNotFound {
		t.Fatalf("other user's visit: status %d, body %s", rec.Code, rec.Body.String())
	}
}

func TestVisitsAreSessionOwned(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()
	h := newTestHandler(t, db, dsn)
	cookie := loginUser(t, db)
	var otherUserID, visitID int64
	if err := db.QueryRow(`INSERT INTO users (username, password_hash) VALUES ('other-gym-user', 'test') RETURNING id`).Scan(&otherUserID); err != nil {
		t.Fatal(err)
	}
	if err := db.QueryRow(`INSERT INTO gym_visits (user_id) VALUES ($1) RETURNING id`, otherUserID).Scan(&visitID); err != nil {
		t.Fatal(err)
	}
	unauthorized := httptest.NewRecorder()
	h.ListVisits(unauthorized, httptest.NewRequest(http.MethodGet, "/gym-visits", nil))
	if unauthorized.Code != http.StatusUnauthorized {
		t.Errorf("visit list without session: status %d, want 401", unauthorized.Code)
	}

	for _, tc := range []struct {
		method, path string
		handler      http.HandlerFunc
		want         int
	}{
		{http.MethodGet, "/gym-visits", h.ListVisits, http.StatusOK},
		{http.MethodDelete, "/gym-visits/{id}", h.DeleteVisit, http.StatusNotFound},
		{http.MethodGet, "/gym-visits/{id}/exercises", h.ListVisitExercises, http.StatusNotFound},
	} {
		req := httptest.NewRequest(tc.method, tc.path, nil)
		req.AddCookie(cookie)
		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(visitID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))
		rec := httptest.NewRecorder()
		tc.handler(rec, req)
		if rec.Code != tc.want {
			t.Errorf("%s %s: status %d, want %d", tc.method, tc.path, rec.Code, tc.want)
		}
		if tc.path == "/gym-visits" && rec.Body.String() != "[]\n" {
			t.Errorf("other user's visit appeared in list: %s", rec.Body.String())
		}
	}
	var count int
	if err := db.QueryRow(`SELECT COUNT(*) FROM gym_visits WHERE id=$1`, visitID).Scan(&count); err != nil || count != 1 {
		t.Fatalf("other user's visit was deleted: count=%d err=%v", count, err)
	}
}

func newTestService(t *testing.T, dsn string) *Service {
	t.Helper()
	pool, err := pgxpool.New(context.Background(), dsn)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(pool.Close)
	return NewService(NewRepository(pool))
}

func newTestHandler(t *testing.T, db *sql.DB, dsn string) *Handler {
	service := newTestService(t, dsn)
	return NewHandler(service, func(r *http.Request) (int64, error) {
		user, err := testhelper.LookupSessionUser(db, r)
		return user.ID, err
	})
}
