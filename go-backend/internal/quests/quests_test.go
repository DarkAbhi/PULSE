package quests

import (
	"testing"
	"time"
)

func TestStreakAndWeekAfterDelayedSync(t *testing.T) {
	today := time.Date(2026, 11, 24, 0, 0, 0, 0, time.UTC)
	days := []HistoryDay{
		{Day: Day{Date: "2026-11-21", Completed: 1, Total: 1}, XP: 30, Hearts: 2},
		{Day: Day{Date: "2026-11-22"}},
		{Day: Day{Date: "2026-11-23", Completed: 1, Total: 2}, XP: 20, Hearts: 1},
		{Day: Day{Date: "2026-11-24", Total: 2}},
	}
	overview := Overview{Today: "2026-11-24", TodayQuests: []Quest{{XP: 20}, {XP: 25}}}
	overview.summarize(days, today, today)
	if p := overview.Progress; p.Streak != 0 || p.BestStreak != 1 || p.TodayXPTarget != 45 {
		t.Fatalf("unexpected progress after a missed day: %+v", p)
	}
	if overview.Week[0].Status != "missed" || overview.Week[1].Status != "pending" || overview.Week[2].Status != "future" {
		t.Fatalf("unexpected week statuses: %+v", overview.Week)
	}

	// A delayed sleep sync completes Monday without counting Sunday (a rest day).
	days[2].Completed++
	days[2].XP += 25
	days[2].Hearts += 2
	overview = Overview{Today: "2026-11-24"}
	overview.summarize(days, today, today.AddDate(0, 0, -7))
	p := overview.Progress
	if p.Streak != 2 || p.BestStreak != 2 || p.LifetimeXP != 75 || p.Hearts != 5 || p.WeekXP != 30 || p.WeekActive != 1 {
		t.Fatalf("unexpected progress after delayed sync: %+v", p)
	}
	if overview.NextWeek != "2026-11-23" || overview.Week[6].Status != "rest" {
		t.Fatalf("unexpected selected week: %+v", overview)
	}
}

func TestLevelCurve(t *testing.T) {
	for xp, want := range map[int]int{0: 1, 99: 1, 100: 2, 299: 2, 300: 3, 600: 4, 4500: 10, 495000: 100, 505000: 101} {
		if got := levelForXP(xp); got != want {
			t.Errorf("levelForXP(%d) = %d, want %d", xp, got, want)
		}
	}
}
