package gym

import (
	"testing"
	"time"

	"github.com/DarkAbhi/life-backend/internal/gym/query"
	"github.com/jackc/pgx/v5/pgtype"
)

func TestBuildOverview(t *testing.T) {
	location, err := time.LoadLocation("Asia/Kolkata")
	if err != nil {
		t.Fatal(err)
	}
	at := func(date string) pgtype.Timestamptz {
		value, err := time.ParseInLocation("2006-01-02 15:04", date, location)
		if err != nil {
			t.Fatal(err)
		}
		return pgtype.Timestamptz{Time: value, Valid: true}
	}
	visit := func(id int64, date string) query.OverviewVisitsRow {
		return query.OverviewVisitsRow{ID: id, CreatedAt: at(date)}
	}
	cases := []struct {
		name   string
		now    string
		data   overviewData
		values []string
	}{
		{"empty on leap day", "2024-02-29 12:00", overviewData{}, []string{
			"No workouts yet", "0.00 %", "0", "0 weeks", "0h 0m", "0 kg", "0", "Not available",
		}},
		{"duplicates and multiple sessions", "2026-01-05 12:00", overviewData{
			Visits: []query.OverviewVisitsRow{
				{ID: 4, CreatedAt: at("2026-01-05 08:00"), Sets: 2, Volume: 118675.5},
				visit(3, "2026-01-05 07:00"), visit(2, "2026-01-01 08:00"), visit(1, "2025-12-23 08:00"),
			},
			Sessions: []query.OverviewSessionsRow{
				{StartTime: at("2026-01-05 07:00"), DurationSeconds: 1800},
				{StartTime: at("2026-01-05 08:00"), DurationSeconds: 900},
				{StartTime: at("2026-01-01 08:00"), DurationSeconds: 60},
			},
		}, []string{"Today", "40.00 %", "3", "3 weeks", "0h 46m", "1,18,675.5 kg", "2", "Morning"}},
		{"current week grace and missing data", "2026-01-05 12:00", overviewData{
			Visits: []query.OverviewVisitsRow{{ID: 1, CreatedAt: at("2026-01-01 08:00"), Sets: 1, UnweightedSets: 1}},
		}, []string{"4 days ago", "20.00 %", "1", "1 week", "Not available", "Not available", "1", "Not available"}},
		{"broken streak and tied preference", "2026-01-19 12:00", overviewData{
			Visits: []query.OverviewVisitsRow{visit(2, "2026-01-01 18:00"), visit(1, "2026-01-01 08:00")},
			Sessions: []query.OverviewSessionsRow{
				{StartTime: at("2026-01-01 08:00"), DurationSeconds: 3600},
				{StartTime: at("2026-01-01 18:00"), DurationSeconds: 3600},
			},
		}, []string{"18 days ago", "5.26 %", "2", "0 weeks", "2h 0m", "Not available", "0", "No preference"}},
		{"old visits excluded from year totals", "2026-01-01 00:10", overviewData{
			Visits: []query.OverviewVisitsRow{{ID: 1, CreatedAt: at("2025-12-31 23:59"), Sets: 10, Volume: 100}},
		}, []string{"1 day ago", "0.00 %", "0", "1 week", "0h 0m", "0 kg", "0", "Not available"}},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			got := buildOverview(tc.data, at(tc.now).Time)
			if len(got.Cards) != 8 {
				t.Fatalf("cards: %d", len(got.Cards))
			}
			for i, want := range tc.values {
				if got.Cards[i].Value != want {
					t.Errorf("%s = %q, want %q", got.Cards[i].ID, got.Cards[i].Value, want)
				}
			}
			if got.Cards[4].Subtitle != "This year" {
				t.Errorf("training subtitle = %q", got.Cards[4].Subtitle)
			}
			if tc.name == "duplicates and multiple sessions" && got.Cards[4].Tooltip != "" {
				t.Errorf("unexpected timing tooltip = %q", got.Cards[4].Tooltip)
			}
			if got.Today != tc.now[:10] {
				t.Errorf("today = %s", got.Today)
			}
			recentCount := 0
			for _, group := range got.RecentWorkouts {
				recentCount += len(group.Workouts)
			}
			if recentCount != min(3, len(tc.data.Visits)) {
				t.Errorf("recent count = %d", recentCount)
			}
			if got.CalendarWorkouts == nil || got.RecentWorkouts == nil {
				t.Error("empty collections must not be null")
			}
			if tc.name == "duplicates and multiple sessions" {
				group := got.CalendarWorkouts["2026-01-05"]
				if len(group.Workouts) != 2 || group.Workouts[0].ID != 4 || group.Workouts[0].TimeLabel != "8:00 AM" {
					t.Errorf("calendar group = %+v", group)
				}
			}
			if tc.name == "current week grace and missing data" {
				if got.Cards[4].Subtitle != "This year" || got.Cards[4].Tooltip != "1 day missing timing" || got.Cards[5].Subtitle != "recorded sets this year · 1 set missing weight" {
					t.Errorf("missing-data labels: %+v", got.Cards)
				}
			}
		})
	}
}
