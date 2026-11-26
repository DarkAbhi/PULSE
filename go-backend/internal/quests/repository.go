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
		if o.Configured {
			if err = reconcile(ctx, tx, userID, day, zone); err != nil {
				return err
			}
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
		       COALESCE(s.activity_rings, false), s.sleep_minutes, COALESCE(s.daily_meals, false),
		       EXISTS (SELECT 1 FROM fitness_workout_schedule WHERE user_id = $1 AND workout_type <> 'rest_day')
		FROM (SELECT 1) base
		CROSS JOIN LATERAL (
			SELECT (clock_timestamp() AT TIME ZONE quest_user_timezone($1))::date AS today
		) local_day
		LEFT JOIN LATERAL (
			-- Show the settings in effect today, matching how daily quests are built. Fall back
			-- to the earliest queued row only when nothing is in effect yet.
			SELECT effective_date, gym_workout, step_target, activity_rings, sleep_minutes, daily_meals
			FROM user_quest_settings WHERE user_id = $1
			ORDER BY effective_date <= local_day.today DESC,
			         CASE WHEN effective_date <= local_day.today THEN effective_date END DESC,
			         effective_date
			LIMIT 1
		) s ON true
	`, userID).Scan(&effective, &view.Settings.GymWorkout, &view.Settings.StepTarget,
		&view.Settings.ActivityRings, &view.Settings.SleepMinutes, &view.Settings.DailyMeals, &view.HasWorkoutDays)
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

func (r *Repository) Activity(ctx context.Context, userID int64) (Activity, error) {
	var activity Activity
	err := pgx.BeginFunc(ctx, r.pool, func(tx pgx.Tx) error {
		today, zone, _, err := questDate(ctx, tx, userID)
		if err != nil {
			return err
		}
		if err = reconcile(ctx, tx, userID, today, zone); err != nil {
			return err
		}

		activity.Today = today.Format(time.DateOnly)
		rows, err := tx.Query(ctx, `
			SELECT day::text, array_agg(quest_id ORDER BY quest_id)
			FROM quest_reward_ledger
			WHERE user_id = $1 AND day BETWEEN $2 AND $3
			GROUP BY day ORDER BY day
		`, userID, today.AddDate(0, 0, 1-activityDays), today)
		if err != nil {
			return err
		}
		activity.Days, err = pgx.CollectRows(rows, func(row pgx.CollectableRow) (ActivityDay, error) {
			var day ActivityDay
			err := row.Scan(&day.Date, &day.CompletedIDs)
			return day, err
		})
		return err
	})
	return activity, err
}

func (r *Repository) SaveSettings(ctx context.Context, userID int64, settings Settings) (string, error) {
	for {
		var effective time.Time
		err := pgx.BeginFunc(ctx, r.pool, func(tx pgx.Tx) error {
			day, zone, reset, err := questDate(ctx, tx, userID)
			if err != nil {
				return err
			}
			effective = day

			// An explicit save supersedes settings queued by the old next-day flow.
			if _, err = tx.Exec(ctx, `
				DELETE FROM user_quest_settings WHERE user_id = $1 AND effective_date > $2
			`, userID, day); err != nil {
				return err
			}
			if _, err = tx.Exec(ctx, `
				INSERT INTO user_quest_settings
					(user_id, effective_date, gym_workout, step_target, activity_rings, sleep_minutes, daily_meals)
				VALUES ($1, $2, $3, $4, $5, $6, $7)
				ON CONFLICT (user_id, effective_date) DO UPDATE SET
					gym_workout = EXCLUDED.gym_workout, step_target = EXCLUDED.step_target,
					activity_rings = EXCLUDED.activity_rings, sleep_minutes = EXCLUDED.sleep_minutes, daily_meals = EXCLUDED.daily_meals
			`, userID, day, settings.GymWorkout, settings.StepTarget, settings.ActivityRings,
				settings.SleepMinutes, settings.DailyMeals); err != nil {
				return err
			}
			if _, err = tx.Exec(ctx, "SELECT quest_open_day($1, $2, $3)", userID, day, zone); err != nil {
				return err
			}
			if err = applyTodayGoals(ctx, tx, userID, day, zone, settings); err != nil {
				return err
			}

			// Retry if the local calendar changed during evaluation.
			var stillToday bool
			err = tx.QueryRow(ctx, `
				SELECT clock_timestamp() < $3 AND quest_user_timezone($1) = $4
				       AND (clock_timestamp() AT TIME ZONE timezone)::date = day
				FROM quest_days WHERE user_id = $1 AND day = $2
			`, userID, day, reset, zone).Scan(&stillToday)
			if err != nil {
				return err
			}
			if !stillToday {
				return errQuestDayRolledOver
			}
			return nil
		})
		if !errors.Is(err, errQuestDayRolledOver) {
			return effective.Format(time.DateOnly), err
		}
	}
}

var errQuestDayRolledOver = errors.New("quest day rolled over while saving goals")

func applyTodayGoals(ctx context.Context, tx pgx.Tx, userID int64, day time.Time, zone string, settings Settings) error {
	// A timezone change can put today's existing snapshot outside its original local
	// day. Align that snapshot before evaluating the explicitly edited goals.
	if _, err := tx.Exec(ctx, `
		UPDATE quest_days SET timezone = $3
		WHERE user_id = $1 AND day = $2 AND (clock_timestamp() AT TIME ZONE timezone)::date <> day
	`, userID, day, zone); err != nil {
		return err
	}

	// The evaluator handles workout and meal goals from their live schedules.
	if _, err := tx.Exec(ctx, `
		WITH goals(quest_id, target) AS (
			VALUES
				('daily_steps', $3::integer),
				('activity_rings', CASE WHEN $4::boolean THEN 3 END),
				('sleep_goal', $5::integer * 60)
		), removed AS (
			DELETE FROM daily_quests q USING goals g
			WHERE q.user_id = $1 AND q.day = $2 AND q.quest_id = g.quest_id AND g.target IS NULL
		)
		INSERT INTO daily_quests (user_id, day, quest_id, target, xp, hearts)
		SELECT $1, $2, g.quest_id, g.target, d.xp, d.hearts
		FROM goals g JOIN quest_definitions d ON d.id = g.quest_id
		WHERE g.target IS NOT NULL
		ON CONFLICT (user_id, day, quest_id) DO UPDATE SET target = EXCLUDED.target, progress = 0
		WHERE daily_quests.target <> EXCLUDED.target
	`, userID, day, settings.StepTarget, settings.ActivityRings, settings.SleepMinutes); err != nil {
		return err
	}

	if _, err := tx.Exec(ctx, "SELECT quest_evaluate_day($1, $2)", userID, day); err != nil {
		return err
	}
	// Deletion cascades for disabled quests. An edited target can also invalidate an
	// existing reward, but a quest that still qualifies keeps its original ledger entry.
	_, err := tx.Exec(ctx, `
		DELETE FROM quest_reward_ledger r USING daily_quests q
		WHERE (r.user_id, r.day, r.quest_id) = (q.user_id, q.day, q.quest_id)
		  AND q.user_id = $1 AND q.day = $2 AND q.progress < q.target
	`, userID, day)
	return err
}
