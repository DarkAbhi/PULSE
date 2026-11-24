package quests

import (
	"context"
	"errors"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type Repository struct {
	pool *pgxpool.Pool
}

func NewRepository(pool *pgxpool.Pool) *Repository {
	return &Repository{pool: pool}
}

func questDate(ctx context.Context, tx pgx.Tx, userID int64) (day time.Time, zone string, reset time.Time, err error) {
	if _, err = tx.Exec(ctx, "SELECT quest_lock($1)", userID); err != nil {
		return
	}
	err = tx.QueryRow(ctx, `
		WITH local_time AS (
			SELECT quest_user_timezone($1) AS zone
		), calendar AS (
			SELECT zone, (clock_timestamp() AT TIME ZONE zone)::date AS day FROM local_time
		)
		SELECT day, zone, (day + 1)::timestamp AT TIME ZONE zone FROM calendar
	`, userID).Scan(&day, &zone, &reset)
	return
}

func (r *Repository) Overview(ctx context.Context, userID int64, selectedWeek time.Time) (Overview, error) {
	o := Overview{TodayQuests: []Quest{}}
	err := pgx.BeginFunc(ctx, r.pool, func(tx pgx.Tx) error {
		day, zone, reset, err := questDate(ctx, tx, userID)
		if err != nil {
			return err
		}
		o.Today = day.Format(time.DateOnly)
		o.NextResetAt = reset.UTC()

		o.SettingsView, err = readSettings(ctx, tx, userID)
		if err != nil {
			return err
		}
		if !o.Configured {
			o.summarize(nil, day, day)
			return nil
		}

		if err = reconcile(ctx, tx, userID, day, zone); err != nil {
			return err
		}

		quests, err := tx.Query(ctx, `
			SELECT q.quest_id AS id, q.target, q.progress, q.xp, q.hearts, r.quest_id IS NOT NULL AS completed
			FROM daily_quests q
			JOIN quest_definitions d ON d.id = q.quest_id
			LEFT JOIN quest_reward_ledger r USING (user_id, day, quest_id)
			WHERE q.user_id = $1 AND q.day = $2 ORDER BY d.display_order
		`, userID, day)
		if err != nil {
			return err
		}
		today, err := pgx.CollectRows(quests, pgx.RowToStructByName[Quest])
		if err != nil {
			return err
		}
		o.TodayQuests = today

		summary, err := readHistory(ctx, tx, userID, nil, day)
		if err != nil {
			return err
		}
		o.summarize(summary, day, selectedWeek)
		return nil
	})
	return o, err
}

type rowQuerier interface {
	QueryRow(context.Context, string, ...any) pgx.Row
}

func readSettings(ctx context.Context, db rowQuerier, userID int64) (SettingsView, error) {
	var view SettingsView
	var effective *time.Time
	err := db.QueryRow(ctx, `
		SELECT s.effective_date, COALESCE(s.gym_workout, false), s.step_target,
		       COALESCE(s.activity_rings, false), s.sleep_minutes, s.meal_target,
		       EXISTS (SELECT 1 FROM fitness_workout_schedule WHERE user_id = $1 AND workout_type <> 'rest_day')
		FROM (SELECT 1) base
		LEFT JOIN LATERAL (
			SELECT effective_date, gym_workout, step_target, activity_rings, sleep_minutes, meal_target
			FROM user_quest_settings WHERE user_id = $1 ORDER BY effective_date DESC LIMIT 1
		) s ON true
	`, userID).Scan(&effective, &view.Settings.GymWorkout, &view.Settings.StepTarget,
		&view.Settings.ActivityRings, &view.Settings.SleepMinutes, &view.Settings.MealTarget, &view.HasWorkoutDays)
	if effective != nil {
		view.Configured = true
		view.SettingsEffective = effective.Format(time.DateOnly)
	}
	return view, err
}

func (r *Repository) Settings(ctx context.Context, userID int64) (SettingsView, error) {
	return readSettings(ctx, r.pool, userID)
}

func reconcile(ctx context.Context, tx pgx.Tx, userID int64, day time.Time, zone string) error {
	if _, err := tx.Exec(ctx, "SELECT quest_open_through($1, $2, $3)", userID, day, zone); err != nil {
		return err
	}
	_, err := tx.Exec(ctx, "SELECT quest_evaluate_day($1, $2)", userID, day)
	return err
}

func readHistory(ctx context.Context, tx pgx.Tx, userID int64, from *time.Time, to time.Time) ([]HistoryDay, error) {
	rows, err := tx.Query(ctx, `
		SELECT d.day::text, count(q.quest_id), count(r.quest_id),
		       COALESCE(sum(r.xp), 0), COALESCE(sum(r.hearts), 0)
		FROM quest_days d
		LEFT JOIN daily_quests q USING (user_id, day)
		LEFT JOIN quest_reward_ledger r USING (user_id, day, quest_id)
		WHERE d.user_id = $1 AND d.day <= $2 AND ($3::date IS NULL OR d.day >= $3)
		GROUP BY d.day ORDER BY d.day
	`, userID, to, from)
	if err != nil {
		return nil, err
	}
	return pgx.CollectRows(rows, func(row pgx.CollectableRow) (HistoryDay, error) {
		var item HistoryDay
		err := row.Scan(&item.Date, &item.Total, &item.Completed, &item.XP, &item.Hearts)
		return item, err
	})
}

func (r *Repository) History(ctx context.Context, userID int64, from, to time.Time) ([]HistoryDay, error) {
	var history []HistoryDay
	err := pgx.BeginFunc(ctx, r.pool, func(tx pgx.Tx) error {
		day, zone, _, err := questDate(ctx, tx, userID)
		if err != nil {
			return err
		}
		if err = reconcile(ctx, tx, userID, day, zone); err != nil {
			return err
		}
		if to.After(day) {
			to = day
		}
		history, err = readHistory(ctx, tx, userID, &from, to)
		if err != nil {
			return err
		}
		today := day.Format(time.DateOnly)
		for i := range history {
			history[i].Status = history[i].statusOn(today)
		}
		return nil
	})
	return history, err
}

func (r *Repository) SaveSettings(ctx context.Context, userID int64, settings Settings) (string, error) {
	var effective time.Time
	err := pgx.BeginFunc(ctx, r.pool, func(tx pgx.Tx) error {
		day, zone, _, err := questDate(ctx, tx, userID)
		if err != nil {
			return err
		}
		var latest time.Time
		var matches bool
		err = tx.QueryRow(ctx, `
			SELECT effective_date,
				ROW(gym_workout, step_target, activity_rings, sleep_minutes, meal_target)
				IS NOT DISTINCT FROM ROW($2::boolean, $3::integer, $4::boolean, $5::integer, $6::integer)
			FROM user_quest_settings WHERE user_id = $1 ORDER BY effective_date DESC LIMIT 1
		`, userID, settings.GymWorkout, settings.StepTarget, settings.ActivityRings,
			settings.SleepMinutes, settings.MealTarget).Scan(&latest, &matches)
		configured := err == nil
		if !configured && !errors.Is(err, pgx.ErrNoRows) {
			return err
		}
		if matches {
			effective = latest
			return nil
		}
		effective = day
		if configured {
			effective = day.AddDate(0, 0, 1)
		}
		if _, err = tx.Exec(ctx, `
			INSERT INTO user_quest_settings
				(user_id, effective_date, gym_workout, step_target, activity_rings, sleep_minutes, meal_target)
			VALUES ($1, $2, $3, $4, $5, $6, $7)
			ON CONFLICT (user_id, effective_date) DO UPDATE SET
				gym_workout = EXCLUDED.gym_workout, step_target = EXCLUDED.step_target,
				activity_rings = EXCLUDED.activity_rings, sleep_minutes = EXCLUDED.sleep_minutes, meal_target = EXCLUDED.meal_target
		`, userID, effective, settings.GymWorkout, settings.StepTarget, settings.ActivityRings,
			settings.SleepMinutes, settings.MealTarget); err != nil {
			return err
		}
		if configured {
			return nil
		}
		_, err = tx.Exec(ctx, "SELECT quest_open_day($1, $2, $3)", userID, day, zone)
		return err
	})
	return effective.Format(time.DateOnly), err
}
