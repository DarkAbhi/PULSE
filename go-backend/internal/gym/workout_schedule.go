package gym

import (
	"context"
	"fmt"
	"strings"
	"unicode/utf8"

	"github.com/DarkAbhi/life-backend/internal/gym/query"
)

type workoutClassification struct {
	WorkoutType  *string `json:"workout_type"`
	WorkoutFocus *string `json:"workout_focus"`
}

type workoutScheduleDay struct {
	Weekday int16 `json:"weekday"`
	workoutClassification
}

func validateWorkoutClassification(workoutType, focus string, allowRest bool) (workoutClassification, error) {
	switch workoutType {
	case "":
		return workoutClassification{}, nil
	case "rest_day":
		if allowRest {
			return workoutClassification{WorkoutType: &workoutType}, nil
		}
		return workoutClassification{}, ValidationError{"Rest days can only be set in the schedule."}
	case "single_muscle", "double_muscle", "push", "pull", "legs",
		"upper_body", "lower_body", "full_body", "cardio", "mobility", "other":
	default:
		return workoutClassification{}, ValidationError{"Choose a valid workout type."}
	}

	focus = strings.TrimSpace(focus)
	if utf8.RuneCountInString(focus) > 120 || strings.ContainsRune(focus, 0) {
		return workoutClassification{}, ValidationError{"Enter a workout focus of up to 120 characters."}
	}
	classification := workoutClassification{WorkoutType: &workoutType}
	if focus != "" {
		classification.WorkoutFocus = &focus
	}
	return classification, nil
}

func (s *Service) UpdateWorkoutClassification(ctx context.Context, userID, visitID int64, workoutType, focus string) error {
	classification, err := validateWorkoutClassification(workoutType, focus, false)
	if err != nil {
		return err
	}
	updated, err := s.store.UpdateWorkoutClassification(ctx, userID, visitID, classification)
	if err != nil {
		return fmt.Errorf("update workout classification: %w", err)
	}
	if !updated {
		return ErrVisitNotFound
	}
	return nil
}

func (s *Service) ListWorkoutSchedule(ctx context.Context, userID int64) ([]workoutScheduleDay, error) {
	days, err := s.store.ListWorkoutSchedule(ctx, userID)
	if err != nil {
		return nil, fmt.Errorf("list workout schedule: %w", err)
	}
	return days, nil
}

func (s *Service) SaveWorkoutScheduleDay(ctx context.Context, userID int64, weekday int16, workoutType, focus string) error {
	if weekday < 1 || weekday > 7 {
		return ValidationError{"Weekday must be between 1 (Monday) and 7 (Sunday)."}
	}
	classification, err := validateWorkoutClassification(workoutType, focus, true)
	if err != nil {
		return err
	}
	if err := s.store.SaveWorkoutScheduleDay(ctx, userID, weekday, classification); err != nil {
		return fmt.Errorf("save workout schedule day: %w", err)
	}
	return nil
}

func (r *Repository) UpdateWorkoutClassification(ctx context.Context, userID, visitID int64, classification workoutClassification) (bool, error) {
	count, err := r.queries.UpdateWorkoutClassification(ctx, query.UpdateWorkoutClassificationParams{
		UserID: userID, VisitID: visitID, WorkoutType: classification.WorkoutType, WorkoutFocus: classification.WorkoutFocus,
	})
	return count > 0, err
}

func (r *Repository) ListWorkoutSchedule(ctx context.Context, userID int64) ([]workoutScheduleDay, error) {
	rows, err := r.queries.ListWorkoutSchedule(ctx, userID)
	if err != nil {
		return nil, err
	}
	days := make([]workoutScheduleDay, 0, len(rows))
	for _, row := range rows {
		days = append(days, workoutScheduleDay{
			Weekday: row.Weekday,
			workoutClassification: workoutClassification{
				WorkoutType: &row.WorkoutType, WorkoutFocus: row.WorkoutFocus,
			},
		})
	}
	return days, nil
}

func (r *Repository) SaveWorkoutScheduleDay(ctx context.Context, userID int64, weekday int16, classification workoutClassification) error {
	if classification.WorkoutType == nil {
		return r.queries.DeleteWorkoutScheduleDay(ctx, query.DeleteWorkoutScheduleDayParams{UserID: userID, Weekday: weekday})
	}
	return r.queries.UpsertWorkoutScheduleDay(ctx, query.UpsertWorkoutScheduleDayParams{
		UserID: userID, Weekday: weekday, WorkoutType: *classification.WorkoutType, WorkoutFocus: classification.WorkoutFocus,
	})
}
