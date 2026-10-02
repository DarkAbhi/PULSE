//go:build integration

package gym

import (
	"context"
	"database/sql"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"github.com/go-chi/chi/v5"
)

func TestOverviewIntegration(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()
	// A workout type with a nonstandard database ID verifies classification by raw value.
	_, err := db.Exec(`
		INSERT INTO users (id, username, password_hash) VALUES (2, 'overview-other', 'test');
		INSERT INTO workout_activity_types (id, raw_value, name) VALUES
			(999, 50, 'Traditional Strength Training'), (998, 37, 'Running');
		INSERT INTO fitness_workouts (id, user_id, uuid, activity_type_id, start_time, end_time, duration_seconds) VALUES
			(10, 1, '00000000-0000-0000-0000-000000000010', 999, '2026-01-01T02:30Z', '2026-01-01T03:00Z', 1800),
			(11, 1, '00000000-0000-0000-0000-000000000011', 999, '2026-01-01T03:30Z', '2026-01-01T03:45Z', 900),
			(12, 2, '00000000-0000-0000-0000-000000000012', 999, '2026-01-01T02:30Z', '2026-01-01T03:00Z', 9999),
			(13, 1, '00000000-0000-0000-0000-000000000013', 998, '2026-01-01T02:30Z', '2026-01-01T03:00Z', 9999),
			(14, 1, '00000000-0000-0000-0000-000000000014', 999, '2026-01-03T02:30Z', '2026-01-03T03:00Z', 9999),
			(15, 1, '00000000-0000-0000-0000-000000000015', 999, '2025-12-31T18:29Z', '2025-12-31T19:00Z', 9999),
			(16, 1, '00000000-0000-0000-0000-000000000016', 999, '2026-01-01T18:31Z', '2026-01-01T19:00Z', 60);
		INSERT INTO gym_visits (id, user_id, created_at, fitness_workout_id) VALUES
			(10, 1, '2026-01-01T02:30Z', 10), (11, 1, '2026-01-01T03:30Z', NULL),
			(12, 2, '2026-01-01T02:30Z', 12), (13, 1, '2027-01-01T02:30Z', NULL),
			(14, 1, '2025-12-31T18:29Z', 15), (15, 1, '2026-01-01T18:31Z', 16);
		INSERT INTO gym_visit_exercises (id, gym_visit_id, name) VALUES (10, 10, 'Squat'), (12, 12, 'Other');
		INSERT INTO gym_exercise_sets (gym_visit_exercise_id, set_number, reps, weight) VALUES
			(10, 1, 10, 20), (10, 2, 5, 30), (10, 3, 5, NULL), (12, 1, 100, 100);
	`)
	if err != nil {
		t.Fatal(err)
	}
	s := newTestService(t, dsn)
	location, err := time.LoadLocation("Asia/Kolkata")
	if err != nil {
		t.Fatal(err)
	}
	now := time.Date(2026, 1, 5, 12, 0, 0, 0, location)
	got, err := s.Overview(context.Background(), 1, now)
	if err != nil {
		t.Fatal(err)
	}
	values := []string{"3 days ago", "40.00 %", "3", "1 week", "0h 46m", "350 kg", "3", "Morning"}
	for i, want := range values {
		if got.Cards[i].Value != want {
			t.Errorf("%s = %q, want %q", got.Cards[i].ID, got.Cards[i].Value, want)
		}
	}
	if len(got.CalendarWorkouts) != 3 || len(got.CalendarWorkouts["2026-01-01"].Workouts) != 2 {
		t.Errorf("calendar visits: %+v", got.CalendarWorkouts)
	}
	if got.Cards[5].Subtitle != "recorded sets this year · 1 set missing weight" {
		t.Errorf("volume subtitle: %s", got.Cards[5].Subtitle)
	}
	// Verify the registered endpoint and session enforcement, including a user with no data.
	for _, tc := range []struct {
		name       string
		userID     int64
		sessionErr error
		status     int
	}{
		{"signed in", 1, nil, http.StatusOK},
		{"no data", 3, nil, http.StatusOK},
		{"no session", 0, sql.ErrNoRows, http.StatusUnauthorized},
	} {
		t.Run(tc.name, func(t *testing.T) {
			h := NewHandler(s, func(*http.Request) (int64, error) { return tc.userID, tc.sessionErr })
			router := chi.NewRouter()
			h.RegisterRoutes(router)
			rec := httptest.NewRecorder()
			router.ServeHTTP(rec, httptest.NewRequest(http.MethodGet, "/fitness/overview", nil))
			if rec.Code != tc.status {
				t.Fatalf("status = %d: %s", rec.Code, rec.Body.String())
			}
			if tc.status == http.StatusOK {
				var response fitnessOverview
				if err := json.Unmarshal(rec.Body.Bytes(), &response); err != nil {
					t.Fatal(err)
				}
				if len(response.Cards) != 8 || rec.Header().Get("Cache-Control") != "no-store" {
					t.Errorf("invalid overview response: %s", rec.Body.String())
				}
				if tc.userID == 3 && len(response.CalendarWorkouts) != 0 {
					t.Error("another user's data leaked")
				}
			}
		})
	}
	if err := s.DeleteVisit(context.Background(), 1, 10); err != nil {
		t.Fatal(err)
	}
	got, err = s.Overview(context.Background(), 1, now)
	if err != nil {
		t.Fatal(err)
	}
	if got.Cards[2].Value != "2" || got.Cards[6].Value != "0" {
		t.Errorf("deleted visit still counted: %+v", got.Cards)
	}
}
