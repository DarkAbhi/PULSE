// Package mealplan manages meal times, planned meals, and consumption status.
package mealplan

import (
	"database/sql"
	"errors"
	"strings"
	"time"
)

type DefaultMealTime struct {
	Name      string
	StartTime string
	EndTime   string
}

var defaultMealTimes = []DefaultMealTime{
	{Name: "Breakfast", StartTime: "07:00:00", EndTime: "10:00:00"},
	{Name: "Lunch", StartTime: "12:00:00", EndTime: "14:30:00"},
	{Name: "Evening Snacks", StartTime: "16:30:00", EndTime: "18:30:00"},
	{Name: "Dinner", StartTime: "19:30:00", EndTime: "22:00:00"},
}

type MealTimeDTO struct {
	ID        int64  `json:"id"`
	Name      string `json:"name"`
	StartTime string `json:"start_time"` // "HH:MM"
	EndTime   string `json:"end_time"`   // "HH:MM"
	IsDefault bool   `json:"is_default"`
	UserID    *int64 `json:"user_id,omitempty"`
}

type CreateMealTimeInput struct {
	Name      string `json:"name"`
	StartTime string `json:"start_time"` // accepts "HH:MM" or "HH:MM:SS"
	EndTime   string `json:"end_time"`   // accepts "HH:MM" or "HH:MM:SS"
}

type MealPlanDTO struct {
	ID           int64     `json:"id"`
	Date         string    `json:"date"`
	Name         string    `json:"name"`
	MealTimeID   *int64    `json:"meal_time_id,omitempty"`
	MealTimeName *string   `json:"meal_time_name,omitempty"`
	StartTime    *string   `json:"start_time,omitempty"`
	EndTime      *string   `json:"end_time,omitempty"`
	IsConsumed   bool      `json:"is_consumed"`
	CreatedAt    time.Time `json:"created_at"`
}

type CreateMealPlanInput struct {
	Date       string `json:"date"`
	Name       string `json:"name"`
	MealTimeID *int64 `json:"meal_time_id,omitempty"`
	IsConsumed *bool  `json:"is_consumed,omitempty"`
}

type UpdateMealPlanConsumedInput struct {
	IsConsumed bool `json:"is_consumed"`
}

type UpdateMealPlanInput struct {
	Date       *string `json:"date,omitempty"`
	Name       *string `json:"name,omitempty"`
	MealTimeID *int64  `json:"meal_time_id,omitempty"`
	IsConsumed *bool   `json:"is_consumed,omitempty"`
}

// parseTimeFlexible accepts "HH:MM" or "HH:MM:SS"
func parseTimeFlexible(val string) (string, error) {
	val = strings.TrimSpace(val)
	if len(val) == 5 {
		if _, err := time.Parse("15:04", val); err != nil {
			return "", err
		}
		return val + ":00", nil
	}
	if len(val) == 8 {
		if _, err := time.Parse("15:04:05", val); err != nil {
			return "", err
		}
		return val, nil
	}
	return "", errors.New("invalid time format, expected HH:MM")
}

func mealPlanDTO(id int64, date, name string, mealTimeID sql.NullInt64, mealTimeName sql.NullString, start, end string, consumed bool, created time.Time) MealPlanDTO {
	item := MealPlanDTO{ID: id, Date: date, Name: name, IsConsumed: consumed, CreatedAt: created}
	if mealTimeID.Valid {
		item.MealTimeID = &mealTimeID.Int64
	}
	if mealTimeName.Valid {
		item.MealTimeName = &mealTimeName.String
	}
	if start != "" {
		item.StartTime = &start
	}
	if end != "" {
		item.EndTime = &end
	}
	return item
}
