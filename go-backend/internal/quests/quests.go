// Package quests projects Quest progress from Pulse records and the reward ledger.
package quests

import "time"

type Settings struct {
	GymWorkout    bool   `json:"gym_workout"`
	StepTarget    *int32 `json:"step_target"`
	ActivityRings bool   `json:"activity_rings"`
	SleepMinutes  *int32 `json:"sleep_minutes"`
	MealTarget    *int32 `json:"meal_target"`
}

func (s Settings) validationError() string {
	switch {
	case s.StepTarget != nil && (*s.StepTarget < 1 || *s.StepTarget > 100000):
		return "Enter a daily step goal between 1 and 100,000."
	case s.SleepMinutes != nil && (*s.SleepMinutes < 1 || *s.SleepMinutes > 1440):
		return "Enter a sleep goal between 1 and 1,440 minutes."
	case s.MealTarget != nil && (*s.MealTarget < 1 || *s.MealTarget > 24):
		return "Enter a daily meal goal between 1 and 24."
	default:
		return ""
	}
}

type Quest struct {
	ID        string `json:"id"`
	Target    int    `json:"target"`
	Progress  int    `json:"progress"`
	XP        int    `json:"xp"`
	Hearts    int    `json:"hearts"`
	Completed bool   `json:"completed"`
}

type Day struct {
	Date      string `json:"date"`
	Status    string `json:"status"`
	Completed int    `json:"completed"`
	Total     int    `json:"total"`
}

type Progress struct {
	LifetimeXP    int `json:"lifetime_xp"`
	Hearts        int `json:"hearts"`
	Level         int `json:"level"`
	TodayXP       int `json:"today_xp"`
	TodayXPTarget int `json:"today_xp_target"`
	TodayComplete int `json:"today_complete"`
	WeekXP        int `json:"week_xp"`
	WeekHearts    int `json:"week_hearts"`
	WeekActive    int `json:"week_active"`
	Streak        int `json:"streak"`
	BestStreak    int `json:"best_streak"`
}

type SettingsView struct {
	Configured        bool     `json:"configured"`
	Settings          Settings `json:"settings"`
	SettingsEffective string   `json:"settings_effective,omitempty"`
	HasWorkoutDays    bool     `json:"has_workout_days"`
}

type Overview struct {
	SettingsView
	Today        string    `json:"today"`
	NextResetAt  time.Time `json:"next_reset_at"`
	Week         []Day     `json:"week"`
	PreviousWeek string    `json:"previous_week,omitempty"`
	NextWeek     string    `json:"next_week,omitempty"`
	TodayQuests  []Quest   `json:"today_quests"`
	Progress     Progress  `json:"progress"`
}

type HistoryDay struct {
	Day
	XP     int `json:"xp"`
	Hearts int `json:"hearts"`
}

func (day HistoryDay) statusOn(today string) string {
	switch {
	case day.Total == 0:
		return "rest"
	case day.Completed == day.Total:
		return "complete"
	case day.Date == today:
		return "pending"
	default:
		return "missed"
	}
}

func monday(day time.Time) time.Time {
	return day.AddDate(0, 0, -(int(day.Weekday())+6)%7)
}

func (o *Overview) summarize(days []HistoryDay, today, selectedWeek time.Time) {
	currentWeek := monday(today)
	weekStart := currentWeek
	if !selectedWeek.IsZero() && !selectedWeek.After(currentWeek) {
		weekStart = monday(selectedWeek)
	}
	weekStartDate := weekStart.Format(time.DateOnly)
	weekEndDate := weekStart.AddDate(0, 0, 6).Format(time.DateOnly)
	byDate := make(map[string]Day, 7)
	p := &o.Progress

	for _, day := range days {
		p.LifetimeXP += day.XP
		p.Hearts += day.Hearts

		day.Status = day.statusOn(o.Today)
		switch day.Status {
		case "complete":
			p.Streak++
			p.BestStreak = max(p.Streak, p.BestStreak)
		case "missed":
			p.Streak = 0
		}

		if day.Date == o.Today {
			p.TodayXP = day.XP
			p.TodayComplete = day.Completed
		}

		if day.Date >= weekStartDate && day.Date <= weekEndDate {
			byDate[day.Date] = day.Day
			p.WeekXP += day.XP
			p.WeekHearts += day.Hearts
			if day.Completed > 0 {
				p.WeekActive++
			}
		}
	}
	p.Level = levelForXP(p.LifetimeXP)
	for _, quest := range o.TodayQuests {
		p.TodayXPTarget += quest.XP
	}

	for i := range 7 {
		date := weekStart.AddDate(0, 0, i).Format(time.DateOnly)
		day, ok := byDate[date]
		if !ok {
			day = Day{Date: date, Status: "unavailable"}
			if date > o.Today {
				day.Status = "future"
			}
		}
		o.Week = append(o.Week, day)
	}
	if len(days) > 0 && days[0].Date < weekStartDate {
		o.PreviousWeek = weekStart.AddDate(0, 0, -7).Format(time.DateOnly)
	}
	if weekStart.Before(currentWeek) {
		o.NextWeek = weekStart.AddDate(0, 0, 7).Format(time.DateOnly)
	}
}

func levelForXP(xp int) int {
	level := 1
	for xp >= level*100 {
		xp -= level * 100
		level++
	}
	return level
}
