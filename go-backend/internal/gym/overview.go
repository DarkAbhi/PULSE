package gym

import (
	"context"
	"fmt"
	"strconv"
	"strings"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgtype"

	"github.com/DarkAbhi/life-backend/internal/gym/query"
	"github.com/DarkAbhi/life-backend/internal/timeutil"
)

type statCard struct {
	ID       string `json:"id"`
	Label    string `json:"label"`
	Value    string `json:"value"`
	Subtitle string `json:"subtitle"`
	Tooltip  string `json:"tooltip,omitempty"`
}

type overviewWorkout struct {
	ID        int64  `json:"id"`
	TimeLabel string `json:"time_label"`
}

type workoutGroup struct {
	Date      string            `json:"date"`
	DateLabel string            `json:"date_label"`
	Workouts  []overviewWorkout `json:"workouts"`
}

type fitnessOverview struct {
	Today            string                  `json:"today"`
	Cards            []statCard              `json:"cards"`
	CalendarWorkouts map[string]workoutGroup `json:"calendar_workouts"`
	RecentWorkouts   []workoutGroup          `json:"recent_workouts"`
}

type overviewData struct {
	Visits   []query.OverviewVisitsRow
	Sessions []query.OverviewSessionsRow
}

func (r *Repository) OverviewData(ctx context.Context, userID int64, start, now time.Time) (overviewData, error) {
	// Read both aggregates from the same snapshot while workouts may be edited.
	tx, err := r.db.BeginTx(ctx, pgx.TxOptions{IsoLevel: pgx.RepeatableRead, AccessMode: pgx.ReadOnly})
	if err != nil {
		return overviewData{}, err
	}
	defer tx.Rollback(ctx)
	q := r.queries.WithTx(tx)
	visits, err := q.OverviewVisits(ctx, query.OverviewVisitsParams{
		UserID: userID, CreatedAt: pgtype.Timestamptz{Time: now, Valid: true},
	})
	if err != nil {
		return overviewData{}, err
	}
	sessions, err := q.OverviewSessions(ctx, query.OverviewSessionsParams{
		UserID:      userID,
		StartTime:   pgtype.Timestamptz{Time: start, Valid: true},
		StartTime_2: pgtype.Timestamptz{Time: now, Valid: true},
	})
	if err != nil {
		return overviewData{}, err
	}
	if err := tx.Commit(ctx); err != nil {
		return overviewData{}, err
	}
	return overviewData{Visits: visits, Sessions: sessions}, nil
}

func (s *Service) Overview(ctx context.Context, userID int64, now time.Time) (fitnessOverview, error) {
	location, err := time.LoadLocation(timeutil.IndiaTimeZone)
	if err != nil {
		return fitnessOverview{}, fmt.Errorf("load fitness timezone: %w", err)
	}
	now = now.In(location)
	start := time.Date(now.Year(), 1, 1, 0, 0, 0, 0, location)
	data, err := s.store.OverviewData(ctx, userID, start, now)
	if err != nil {
		return fitnessOverview{}, fmt.Errorf("load fitness overview: %w", err)
	}
	return buildOverview(data, now), nil
}

func buildOverview(data overviewData, now time.Time) fitnessOverview {
	const dateFormat = "2006-01-02"
	today := time.Date(now.Year(), now.Month(), now.Day(), 0, 0, 0, 0, now.Location())
	start := time.Date(now.Year(), 1, 1, 0, 0, 0, 0, now.Location())
	result := fitnessOverview{
		Today: today.Format(dateFormat), CalendarWorkouts: make(map[string]workoutGroup),
		RecentWorkouts: make([]workoutGroup, 0),
	}
	days := make(map[string]bool)
	weeks := make(map[string]bool)
	var count, sets, unweighted int64
	var volume float64
	for i, visit := range data.Visits {
		local := visit.CreatedAt.Time.In(now.Location())
		date := local.Format(dateFormat)
		workout := overviewWorkout{ID: visit.ID, TimeLabel: local.Format("3:04 PM")}
		group := result.CalendarWorkouts[date]
		group.Date, group.DateLabel = date, local.Format("Monday, 2 January 2006")
		group.Workouts = append(group.Workouts, workout)
		result.CalendarWorkouts[date] = group
		if i < 3 {
			last := len(result.RecentWorkouts) - 1
			if last >= 0 && result.RecentWorkouts[last].Date == date {
				result.RecentWorkouts[last].Workouts = append(result.RecentWorkouts[last].Workouts, workout)
			} else {
				result.RecentWorkouts = append(result.RecentWorkouts, workoutGroup{
					Date: date, DateLabel: group.DateLabel, Workouts: []overviewWorkout{workout},
				})
			}
		}
		weeks[monday(local).Format(dateFormat)] = true
		if !local.Before(start) {
			count++
			days[date] = true
			sets += visit.Sets
			unweighted += visit.UnweightedSets
			volume += visit.Volume
		}
	}
	lastWorkout := "No workouts yet"
	if len(data.Visits) > 0 {
		last := data.Visits[0].CreatedAt.Time.In(now.Location())
		lastDay := time.Date(last.Year(), last.Month(), last.Day(), 0, 0, 0, 0, now.Location())
		daysAgo := int(today.Sub(lastDay).Hours() / 24)
		lastWorkout = plural(daysAgo, "day") + " ago"
		if daysAgo == 0 {
			lastWorkout = "Today"
		}
	}
	week := monday(today)
	if !weeks[week.Format(dateFormat)] {
		week = week.AddDate(0, 0, -7)
	}
	streak := 0
	for weeks[week.Format(dateFormat)] {
		streak++
		week = week.AddDate(0, 0, -7)
	}
	var duration float64
	timedDays := make(map[string]bool)
	periods := [4]int{}
	for _, session := range data.Sessions {
		local := session.StartTime.Time.In(now.Location())
		timedDays[local.Format(dateFormat)] = true
		duration += session.DurationSeconds
		switch hour := local.Hour(); {
		case hour >= 5 && hour < 12:
			periods[0]++
		case hour >= 12 && hour < 17:
			periods[1]++
		case hour >= 17 && hour < 21:
			periods[2]++
		default:
			periods[3]++
		}
	}
	preference, maximum, ties := "Not available", 0, 0
	for i, n := range periods {
		if n > maximum {
			preference, maximum, ties = []string{"Morning", "Afternoon", "Evening", "Night"}[i], n, 1
		} else if n == maximum && n > 0 {
			ties++
		}
	}
	if ties > 1 {
		preference = "No preference"
	}
	minutes := int64(duration / 60)
	trained := fmt.Sprintf("%dh %dm", minutes/60, minutes%60)
	timingTooltip := ""
	if missing := len(days) - len(timedDays); missing > 0 {
		timingTooltip = plural(missing, "day") + " missing timing"
		if len(data.Sessions) == 0 {
			trained = "Not available"
		}
	}
	volumeLabel := indianNumber(volume) + " kg"
	volumeSubtitle := "recorded sets this year"
	if unweighted > 0 {
		volumeSubtitle += " · " + plural(int(unweighted), "set") + " missing weight"
		if sets == unweighted {
			volumeLabel = "Not available"
		}
	} else if sets == 0 && count > 0 {
		volumeLabel = "Not available"
	}
	result.Cards = []statCard{
		{"last_workout", "Last worked out", lastWorkout, "at gym", ""},
		{"attendance", "Gym attendance", fmt.Sprintf("%.2f %%", float64(len(days))*100/float64(now.YearDay())), "of days so far this year", ""},
		{"workouts", "Total workouts", strconv.FormatInt(count, 10), "this year", ""},
		{"streak", "Workout streak", plural(streak, "week"), "with a gym visit", ""},
		{"trained", "Total trained", trained, "This year", timingTooltip},
		{"volume", "Volume lifted", volumeLabel, volumeSubtitle, ""},
		{"sets", "Sets", strconv.FormatInt(sets, 10), "recorded this year", ""},
		{"preference", "Preferred workout time", preference, "from recorded starts this year", ""},
	}
	return result
}

func monday(t time.Time) time.Time {
	day := time.Date(t.Year(), t.Month(), t.Day(), 0, 0, 0, 0, t.Location())
	return day.AddDate(0, 0, -(int(day.Weekday())+6)%7)
}

func plural(n int, unit string) string {
	if n != 1 {
		unit += "s"
	}
	return fmt.Sprintf("%d %s", n, unit)
}

func indianNumber(value float64) string {
	parts := strings.Split(fmt.Sprintf("%.2f", value), ".")
	whole := parts[0]
	if len(whole) > 3 {
		prefix, tail := whole[:len(whole)-3], whole[len(whole)-3:]
		for len(prefix) > 2 {
			tail = prefix[len(prefix)-2:] + "," + tail
			prefix = prefix[:len(prefix)-2]
		}
		whole = prefix + "," + tail
	}
	fraction := strings.TrimRight(parts[1], "0")
	if fraction != "" {
		whole += "." + fraction
	}
	return whole
}
