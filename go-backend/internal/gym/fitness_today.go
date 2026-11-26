package gym

import (
	"context"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5/pgtype"

	"github.com/DarkAbhi/life-backend/internal/gym/query"
)

type FitnessTodaySummary struct {
	Date                string   `json:"date"`
	StepsCount          *int32   `json:"steps_count"`
	MoveCalories        *float64 `json:"move_calories"`
	MoveCaloriesGoal    *float64 `json:"move_calories_goal"`
	ExerciseMinutes     *int32   `json:"exercise_minutes"`
	ExerciseMinutesGoal *int32   `json:"exercise_minutes_goal"`
	StandHours          *int32   `json:"stand_hours"`
	StandHoursGoal      *int32   `json:"stand_hours_goal"`
	TotalAsleepSeconds  *int32   `json:"total_asleep_seconds"`
}

func (s *Service) FitnessToday(ctx context.Context, userID int64, now time.Time) (FitnessTodaySummary, error) {
	location, err := s.location(ctx, userID)
	if err != nil {
		return FitnessTodaySummary{}, err
	}
	summary, err := s.store.FitnessToday(ctx, userID, now.In(location))
	if err != nil {
		return FitnessTodaySummary{}, fmt.Errorf("load today's fitness summary: %w", err)
	}
	return summary, nil
}

func (r *Repository) FitnessToday(ctx context.Context, userID int64, day time.Time) (FitnessTodaySummary, error) {
	row, err := r.queries.FitnessToday(ctx, query.FitnessTodayParams{
		UserID: userID,
		Day:    pgtype.Date{Time: day, Valid: true},
	})
	if err != nil {
		return FitnessTodaySummary{}, err
	}
	return FitnessTodaySummary{
		Date:                day.Format(time.DateOnly),
		StepsCount:          row.StepsCount,
		MoveCalories:        row.MoveCalories,
		MoveCaloriesGoal:    row.MoveCaloriesGoal,
		ExerciseMinutes:     row.ExerciseMinutes,
		ExerciseMinutesGoal: row.ExerciseMinutesGoal,
		StandHours:          row.StandHours,
		StandHoursGoal:      row.StandHoursGoal,
		TotalAsleepSeconds:  row.TotalAsleepSeconds,
	}, nil
}
