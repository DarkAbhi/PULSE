// Package gym records gym visits, exercises, sets, and visit reminders.
package gym

import "time"

const reminderSource = "Gym reminder"

type visitListItem struct {
	ID        int64     `json:"id"`
	CreatedAt time.Time `json:"created_at"`
}

type exerciseSetInput struct {
	Reps   int      `json:"reps"`
	Weight *float64 `json:"weight"`
}

type createExerciseBody struct {
	Name string             `json:"name"`
	Sets []exerciseSetInput `json:"sets"`
}

type exerciseSetDTO struct {
	ID        int64    `json:"id"`
	SetNumber int      `json:"set_number"`
	Reps      int      `json:"reps"`
	Weight    *float64 `json:"weight"`
}

type exerciseDTO struct {
	ID   int64            `json:"id"`
	Name string           `json:"name"`
	Sets []exerciseSetDTO `json:"sets"`
}
