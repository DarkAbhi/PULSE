package quests

import (
	"context"
	_ "embed"
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/jackc/pgx/v5"

	"github.com/DarkAbhi/life-backend/internal/webutil"
)

//go:embed debug_seed.sql
var debugSeedSQL string

type debugSeedResult struct {
	Days        int `json:"days"`
	Completions int `json:"completions"`
}

func (h *Handler) RegisterDebugRoutes(r chi.Router) {
	r.Get("/debug/settings", h.authenticated(func(w http.ResponseWriter, _ *http.Request, _ int64) {
		w.WriteHeader(http.StatusNoContent)
	}))
	r.Post("/debug/quest-history", h.authenticated(h.seedQuestHistory))
}

func (h *Handler) seedQuestHistory(w http.ResponseWriter, r *http.Request, userID int64) {
	result, err := h.repository.seedHistory(r.Context(), userID)
	if err != nil {
		webutil.ServerErrorContext(r.Context(), w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, result)
}

func (r *Repository) seedHistory(ctx context.Context, userID int64) (debugSeedResult, error) {
	var result debugSeedResult
	err := pgx.BeginFunc(ctx, r.pool, func(tx pgx.Tx) error {
		today, zone, _, err := questDate(ctx, tx, userID)
		if err != nil {
			return err
		}
		// Exclude dates with any existing snapshot or source data. Use the same
		// user lock as quest reconciliation while creating the historical records.
		if _, err := tx.Exec(ctx, `
			CREATE TEMP TABLE debug_quest_days ON COMMIT DROP AS
			WITH calendar AS (
				SELECT $1::bigint AS user_id, $2::date - days_ago AS day, $3::text AS zone
				FROM generate_series(1, $4::integer) AS offsets(days_ago)
			), dates AS (
				SELECT *, day::timestamp AT TIME ZONE zone AS start_at,
				          (day + 1)::timestamp AT TIME ZONE zone AS end_at,
				          get_byte(decode(md5(day::text), 'hex'), 0) AS sample
				FROM calendar
			)
			SELECT *, CASE WHEN sample % 13 < 2 THEN 0 ELSE sample % 32 END AS goals
			FROM dates d
			WHERE NOT EXISTS (SELECT FROM quest_days q WHERE q.user_id = d.user_id AND q.day = d.day)
			  AND NOT EXISTS (SELECT FROM meal_plans m WHERE m.user_id = d.user_id AND m.date = d.day)
			  AND NOT EXISTS (SELECT FROM fitness_activity_rings r WHERE r.user_id = d.user_id AND r.summary_date = d.day)
			  AND NOT EXISTS (SELECT FROM fitness_sleep s WHERE s.user_id = d.user_id AND s.sleep_date = d.day)
			  AND NOT EXISTS (SELECT FROM gym_visits g WHERE g.user_id = d.user_id AND g.created_at >= d.start_at AND g.created_at < d.end_at)
			  AND NOT EXISTS (SELECT FROM fitness_workouts w WHERE w.user_id = d.user_id AND w.start_time >= d.start_at AND w.start_time < d.end_at)
		`, userID, today, zone, activityDays-1); err != nil {
			return err
		}
		if _, err = tx.Exec(ctx, debugSeedSQL); err != nil {
			return err
		}
		return tx.QueryRow(ctx, `
			SELECT count(DISTINCT d.day), count(r.quest_id)
			FROM pg_temp.debug_quest_days d LEFT JOIN quest_reward_ledger r USING (user_id, day)
		`).Scan(&result.Days, &result.Completions)
	})
	return result, err
}
