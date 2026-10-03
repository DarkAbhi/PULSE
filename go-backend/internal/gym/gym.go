// Package gym records gym visits, exercises, and sets.
package gym

import (
	"encoding/json"
	"time"
)

type visitListItem struct {
	ID        int64     `json:"id"`
	CreatedAt time.Time `json:"created_at"`
}

type visitDetail struct {
	visitListItem
	StartTime       *time.Time `json:"start_time"`
	EndTime         *time.Time `json:"end_time"`
	DurationSeconds *float64   `json:"duration_seconds"`
	CaloriesBurned  *float64   `json:"calories_burned"`
}

type exerciseSetInput struct {
	Reps   int      `json:"reps"`
	Weight *float64 `json:"weight"`
}

type createExerciseBody struct {
	Name              string             `json:"name"`
	ExerciseCatalogID *string            `json:"exercise_catalog_id"`
	RememberAlias     bool               `json:"remember_alias"`
	Sets              []exerciseSetInput `json:"sets"`
}

type createExercisesBody struct {
	Exercises []createExerciseBody `json:"exercises"`
}

type exerciseSetDTO struct {
	ID        int64    `json:"id"`
	SetNumber int      `json:"set_number"`
	Reps      int      `json:"reps"`
	Weight    *float64 `json:"weight"`
}

type exerciseDTO struct {
	ID                int64            `json:"id"`
	Name              string           `json:"name"`
	ExerciseCatalogID *string          `json:"exercise_catalog_id"`
	CatalogExercise   *catalogExercise `json:"catalog_exercise"`
	Sets              []exerciseSetDTO `json:"sets"`
}

type catalogExercise struct {
	ID        string          `json:"id"`
	Name      string          `json:"name"`
	Equipment *string         `json:"equipment"`
	Data      json.RawMessage `json:"data"`
}

type catalogCandidate struct {
	catalogExercise
	MatchType string `json:"match_type"`
}

type catalogSearch struct {
	Query             string             `json:"query"`
	MatchType         string             `json:"match_type"`
	ExerciseCatalogID *string            `json:"exercise_catalog_id"`
	Candidates        []catalogCandidate `json:"candidates"`
}
