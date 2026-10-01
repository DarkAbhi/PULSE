//go:build integration

package gym

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strconv"
	"strings"
	"testing"
	"time"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"github.com/go-chi/chi/v5"
)

func TestGetVisitDetails(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()
	userID := int64(1)
	h := NewHandler(newTestService(t, dsn), func(*http.Request) (int64, error) { return userID, nil })
	router := chi.NewRouter()
	h.RegisterRoutes(router)
	var activityID, otherUserID int64
	if err := db.QueryRow(`INSERT INTO workout_activity_types (raw_value, name) VALUES (9999, 'Test workout') RETURNING id`).Scan(&activityID); err != nil {
		t.Fatal(err)
	}
	if err := db.QueryRow(`INSERT INTO users (username, password_hash) VALUES ('workout-details-other', 'test') RETURNING id`).Scan(&otherUserID); err != nil {
		t.Fatal(err)
	}
	start := time.Date(2026, 10, 1, 18, 0, 0, 0, time.UTC)
	end := start.Add(time.Hour)
	var workoutID, linkedVisitID, manualVisitID int64
	if err := db.QueryRow(`INSERT INTO fitness_workouts
		(user_id, uuid, activity_type_id, start_time, end_time, duration_seconds, calories_burned)
		VALUES (1, '00000000-0000-0000-0000-000000000001', $1, $2, $3, 3600.125, 0) RETURNING id`,
		activityID, start, end).Scan(&workoutID); err != nil {
		t.Fatal(err)
	}
	if err := db.QueryRow(`INSERT INTO gym_visits (user_id, fitness_workout_id) VALUES (1, $1) RETURNING id`, workoutID).Scan(&linkedVisitID); err != nil {
		t.Fatal(err)
	}
	if err := db.QueryRow(`INSERT INTO gym_visits (user_id) VALUES (1) RETURNING id`).Scan(&manualVisitID); err != nil {
		t.Fatal(err)
	}
	get := func(id string) *httptest.ResponseRecorder {
		rec := httptest.NewRecorder()
		router.ServeHTTP(rec, httptest.NewRequest(http.MethodGet, "/gym-visits/"+id, nil))
		return rec
	}
	decode := func(id int64) visitDetail {
		t.Helper()
		rec := get(strconv.FormatInt(id, 10))
		if rec.Code != http.StatusOK {
			t.Fatalf("status %d: %s", rec.Code, rec.Body.String())
		}
		var detail visitDetail
		if err := json.Unmarshal(rec.Body.Bytes(), &detail); err != nil {
			t.Fatal(err)
		}
		return detail
	}
	t.Run("linked workout including zero calories", func(t *testing.T) {
		detail := decode(linkedVisitID)
		if detail.ID != linkedVisitID || detail.StartTime == nil || !detail.StartTime.Equal(start) ||
			detail.EndTime == nil || !detail.EndTime.Equal(end) || detail.DurationSeconds == nil ||
			*detail.DurationSeconds != 3600.125 || detail.CaloriesBurned == nil || *detail.CaloriesBurned != 0 {
			t.Fatalf("unexpected detail: %+v", detail)
		}
	})
	t.Run("manual visit has no metrics", func(t *testing.T) {
		detail := decode(manualVisitID)
		if detail.ID != manualVisitID || detail.StartTime != nil || detail.EndTime != nil || detail.DurationSeconds != nil || detail.CaloriesBurned != nil {
			t.Fatalf("unexpected manual detail: %+v", detail)
		}
	})
	t.Run("missing calories stay null", func(t *testing.T) {
		if _, err := db.Exec(`UPDATE fitness_workouts SET calories_burned=NULL WHERE id=$1`, workoutID); err != nil {
			t.Fatal(err)
		}
		if detail := decode(linkedVisitID); detail.CaloriesBurned != nil || detail.DurationSeconds == nil {
			t.Fatalf("unexpected nullable calories: %+v", detail)
		}
	})
	t.Run("other user's linked workout is hidden", func(t *testing.T) {
		if _, err := db.Exec(`UPDATE fitness_workouts SET user_id=$1 WHERE id=$2`, otherUserID, workoutID); err != nil {
			t.Fatal(err)
		}
		detail := decode(linkedVisitID)
		if detail.StartTime != nil || detail.EndTime != nil || detail.DurationSeconds != nil || detail.CaloriesBurned != nil {
			t.Fatalf("other user's metrics exposed: %+v", detail)
		}
	})
	t.Run("other user's visit is not found", func(t *testing.T) {
		userID = otherUserID
		defer func() { userID = 1 }()
		if rec := get(strconv.FormatInt(linkedVisitID, 10)); rec.Code != http.StatusNotFound {
			t.Fatalf("other user's visit: status %d", rec.Code)
		}
	})
	t.Run("missing and invalid visits", func(t *testing.T) {
		if rec := get("9223372036854775807"); rec.Code != http.StatusNotFound {
			t.Fatalf("missing visit: status %d", rec.Code)
		}
		if rec := get("invalid"); rec.Code != http.StatusBadRequest {
			t.Fatalf("invalid visit: status %d", rec.Code)
		}
	})
}

func TestAddVisitOnDate(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()
	h := newTestHandler(t, db, dsn)
	cookie := loginUser(t, db)
	router := chi.NewRouter()
	h.RegisterRoutes(router)
	post := func(body string, authenticated bool) *httptest.ResponseRecorder {
		t.Helper()
		req := httptest.NewRequest(http.MethodPost, "/gym-visits", strings.NewReader(body))
		if authenticated {
			req.AddCookie(cookie)
		}
		rec := httptest.NewRecorder()
		router.ServeHTTP(rec, req)
		return rec
	}
	t.Run("selected date in India time", func(t *testing.T) {
		rec := post(`{"date":"2020-02-29"}`, true)
		if rec.Code != http.StatusCreated {
			t.Fatalf("status %d: %s", rec.Code, rec.Body.String())
		}
		var body struct {
			ID int64 `json:"id"`
		}
		if err := json.Unmarshal(rec.Body.Bytes(), &body); err != nil {
			t.Fatal(err)
		}
		var ownerID int64
		var createdAt time.Time
		if err := db.QueryRow(`SELECT user_id, created_at FROM gym_visits WHERE id=$1`, body.ID).Scan(&ownerID, &createdAt); err != nil {
			t.Fatal(err)
		}
		want := time.Date(2020, 2, 28, 18, 30, 0, 0, time.UTC)
		if ownerID != 1 || !createdAt.Equal(want) {
			t.Fatalf("owner=%d date=%s, want owner=1 date=%s", ownerID, createdAt, want)
		}
	})
	t.Run("today uses current time", func(t *testing.T) {
		location, err := time.LoadLocation("Asia/Kolkata")
		if err != nil {
			t.Fatal(err)
		}
		before := time.Now().Add(-time.Second)
		body := `{"date":"` + before.In(location).Format("2006-01-02") + `"}`
		rec := post(body, true)
		if rec.Code != http.StatusCreated {
			t.Fatalf("status %d: %s", rec.Code, rec.Body.String())
		}
		var createdAt time.Time
		if err := db.QueryRow(`SELECT created_at FROM gym_visits ORDER BY id DESC LIMIT 1`).Scan(&createdAt); err != nil {
			t.Fatal(err)
		}
		if createdAt.Before(before) || createdAt.After(time.Now()) {
			t.Fatalf("today's timestamp is not current: %s", createdAt)
		}
	})
	for _, tc := range []struct{ name, body string }{
		{"invalid day", `{"date":"2026-02-30"}`},
		{"invalid format", `{"date":"01/10/2026"}`},
		{"missing date", `{}`},
		{"invalid JSON", `not-json`},
	} {
		t.Run(tc.name, func(t *testing.T) {
			if rec := post(tc.body, true); rec.Code != http.StatusBadRequest {
				t.Fatalf("status %d, want 400", rec.Code)
			}
		})
	}
	t.Run("requires session", func(t *testing.T) {
		if rec := post(`{"date":"2020-02-29"}`, false); rec.Code != http.StatusUnauthorized {
			t.Fatalf("status %d, want 401", rec.Code)
		}
	})
	var count int
	if err := db.QueryRow(`SELECT COUNT(*) FROM gym_visits`).Scan(&count); err != nil || count != 2 {
		t.Fatalf("unexpected visits after rejected requests: count=%d err=%v", count, err)
	}
}
