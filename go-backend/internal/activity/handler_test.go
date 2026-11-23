package activity

import (
	"database/sql"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/go-chi/chi/v5"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
)

func TestRecentActivityRequiresSession(t *testing.T) {
	router := chi.NewRouter()
	NewHandler(nil, func(*http.Request) (int64, error) { return 0, sql.ErrNoRows }).RegisterRoutes(router)
	response := httptest.NewRecorder()
	router.ServeHTTP(response, httptest.NewRequest(http.MethodGet, "/activity/recent", nil))
	if response.Code != http.StatusUnauthorized {
		t.Fatalf("status = %d, want 401", response.Code)
	}
}

func TestRecentActivitySourceLifecycle(t *testing.T) {
	db, dsn, stop := testhelper.StartPostgresWithDSN(t)
	t.Cleanup(stop)
	pool, err := pgxpool.New(t.Context(), dsn)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(pool.Close)

	exec := func(statement string) {
		t.Helper()
		if _, err := db.ExecContext(t.Context(), statement); err != nil {
			t.Fatal(err)
		}
	}
	exec(`
		INSERT INTO fitness_activity_rings (user_id, summary_date, steps_count)
		VALUES (1, '2026-11-23', 100);
		INSERT INTO fitness_sleep (user_id, sleep_date, total_asleep_seconds, main_sleep_seconds, wake_at, sessions)
		VALUES (1, '2026-11-23', 25200, 25200, '2026-11-23T06:00:00Z', '[{}]');
		INSERT INTO workout_activity_types (raw_value, name) VALUES (50, 'Traditional Strength Training');
		INSERT INTO fitness_workouts (user_id, uuid, activity_type_id, start_time, end_time, duration_seconds)
		SELECT 1, '32d09d06-f2e6-4676-b872-f68f4f49d540', id, '2026-11-23T09:00Z', '2026-11-23T10:00Z', 3600
		FROM workout_activity_types WHERE raw_value = 50;
		INSERT INTO gym_visits (user_id, fitness_workout_id)
		SELECT 1, id FROM fitness_workouts WHERE user_id = 1;
	`)

	router := chi.NewRouter()
	NewHandler(pool, func(*http.Request) (int64, error) { return 1, nil }).RegisterRoutes(router)
	read := func() []Entry {
		t.Helper()
		response := httptest.NewRecorder()
		router.ServeHTTP(response, httptest.NewRequest(http.MethodGet, "/activity/recent", nil))
		if response.Code != http.StatusOK || response.Header().Get("Cache-Control") != "no-store" {
			t.Fatalf("response = %d, %s", response.Code, response.Body)
		}
		var entries []Entry
		if err := json.Unmarshal(response.Body.Bytes(), &entries); err != nil || entries == nil {
			t.Fatalf("invalid entries %s: %v", response.Body, err)
		}
		return entries
	}
	first := read()
	if len(first) != 3 || first[0].Kind != "workout_synced" ||
		first[1].Kind != "sleep_synced" || first[2].Kind != "daily_activity_synced" {
		t.Fatalf("initial sync entries: %+v", first)
	}

	replayOrUpdate := `
		INSERT INTO fitness_activity_rings (user_id, summary_date, steps_count)
		VALUES (1, '2026-11-23', 200)
		ON CONFLICT (user_id, summary_date) DO UPDATE SET steps_count = EXCLUDED.steps_count, updated_at = NOW();
		INSERT INTO fitness_sleep (user_id, sleep_date, total_asleep_seconds, main_sleep_seconds, wake_at, sessions)
		VALUES (1, '2026-11-23', 28800, 28800, '2026-11-23T07:00:00Z', '[{}]')
		ON CONFLICT (user_id, sleep_date) DO UPDATE SET total_asleep_seconds = EXCLUDED.total_asleep_seconds,
		  main_sleep_seconds = EXCLUDED.main_sleep_seconds, wake_at = EXCLUDED.wake_at;
		INSERT INTO fitness_workouts (user_id, uuid, activity_type_id, start_time, end_time, duration_seconds)
		SELECT 1, '32d09d06-f2e6-4676-b872-f68f4f49d540', id, '2026-11-23T09:00Z', '2026-11-23T10:00Z', 3600
		FROM workout_activity_types WHERE raw_value = 50
		ON CONFLICT (user_id, uuid) DO UPDATE SET updated_at = NOW();
	`
	exec(replayOrUpdate)
	exec(replayOrUpdate)
	afterReplay := read()
	if len(afterReplay) != len(first) {
		t.Fatalf("after replay: %+v", afterReplay)
	}
	for i, entry := range afterReplay {
		if entry.ID != first[i].ID || !entry.RecordedAt.Equal(first[i].RecordedAt) {
			t.Fatalf("sync changed activity order or identity: %+v", afterReplay)
		}
	}

	var metrics struct {
		Steps int `json:"steps"`
	}
	if err := json.Unmarshal(afterReplay[2].Details, &metrics); err != nil || metrics.Steps != 200 {
		t.Fatalf("updated rings details = %s, %v", afterReplay[2].Details, err)
	}

	exec(`
		INSERT INTO users (username, password_hash) VALUES ('activity-other', 'unused');
		INSERT INTO meal_plans (user_id, date, name) SELECT id, '2026-11-23', 'Other account meal'
		  FROM users WHERE username = 'activity-other';
		INSERT INTO meal_plans (user_id, date, name) VALUES (1, '2026-11-23', 'Test meal');
		UPDATE meal_plans SET is_consumed = TRUE WHERE user_id = 1;
		UPDATE meal_plans SET name = 'Updated meal', is_consumed = TRUE WHERE user_id = 1;
	`)
	exec(`
		BEGIN;
		INSERT INTO meal_plans (user_id, date, name) VALUES (1, '2026-11-23', 'Rolled-back meal');
		ROLLBACK;
	`)
	withMeal := read()
	if len(withMeal) != 3 || withMeal[0].Kind != "meal_eaten" ||
		withMeal[1].Kind != "meal_added" || withMeal[2].Kind != "workout_synced" {
		t.Fatalf("meal entries: %+v", withMeal)
	}
	exec(`
		DELETE FROM meal_plans WHERE user_id = 1;
		DELETE FROM fitness_activity_rings WHERE user_id = 1;
		DELETE FROM fitness_sleep WHERE user_id = 1;
		DELETE FROM gym_visits WHERE user_id = 1;
		DELETE FROM fitness_workouts WHERE user_id = 1;
	`)
	if remaining := read(); len(remaining) != 0 {
		t.Fatalf("after deleting the source records: %+v", remaining)
	}

	exec(`
		INSERT INTO financial_accounts (user_id, display_name, account_type)
		VALUES (1, 'HDFC Millennia', 'credit_card');
		INSERT INTO statement_import_batches (id, user_id, account_id, source_sha256,
		    status, profile_id, currency, account_confirmed, expires_at)
		SELECT '1f15507c-6e45-4440-8db7-373216d5eec8', 1, id, repeat('a', 64),
		    'ready', 'test', 'INR', true, NOW() + interval '1 hour'
		FROM financial_accounts WHERE user_id = 1;
		INSERT INTO financial_horizon_transactions (user_id, name, amount, type, source, account_id,
		    import_batch_id, import_row_ordinal, dedupe_hash)
		SELECT 1, 'Imported transaction', 125, 'debit', 'statement', account_id, id, 1, repeat('b', 64)
		FROM statement_import_batches WHERE user_id = 1;
		UPDATE statement_import_batches SET status = 'committed', imported_count = 1 WHERE user_id = 1;
		UPDATE statement_import_batches SET status = 'committed' WHERE user_id = 1;
	`)
	withImport := read()
	if len(withImport) != 2 || withImport[0].Kind != "statement_imported" || withImport[1].Kind != "account_added" {
		t.Fatalf("statement import entries: %+v", withImport)
	}
	var account struct {
		Name string `json:"name"`
	}
	if err := json.Unmarshal(withImport[0].Details, &account); err != nil || account.Name != "HDFC Millennia" {
		t.Fatalf("statement account = %s, %v", withImport[0].Details, err)
	}
	exec(`
		DELETE FROM financial_horizon_transactions WHERE user_id = 1;
		DELETE FROM statement_import_batches WHERE user_id = 1;
	`)
	if remaining := read(); len(remaining) != 1 || remaining[0].Kind != "account_added" {
		t.Fatalf("after deleting the statement import: %+v", remaining)
	}
}
