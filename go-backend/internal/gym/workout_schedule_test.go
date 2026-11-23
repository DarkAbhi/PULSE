package gym

import (
	"context"
	"database/sql"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/go-chi/chi/v5"
)

type workoutScheduleTestStore struct {
	store
	classification workoutClassification
	days           map[int16]workoutClassification
}

func (s *workoutScheduleTestStore) UpdateWorkoutClassification(_ context.Context, userID, visitID int64, classification workoutClassification) (bool, error) {
	if userID != 4 || visitID != 12 {
		return false, nil
	}
	s.classification = classification
	return true, nil
}

func (s *workoutScheduleTestStore) SaveWorkoutScheduleDay(_ context.Context, userID int64, weekday int16, classification workoutClassification) error {
	if userID == 4 {
		s.days[weekday] = classification
	}
	return nil
}

func TestWorkoutClassificationAndScheduleRoutes(t *testing.T) {
	data := &workoutScheduleTestStore{days: make(map[int16]workoutClassification)}
	handler := NewHandler(NewService(data), func(r *http.Request) (int64, error) {
		if r.Header.Get("Authorization") == "" {
			return 0, sql.ErrNoRows
		}
		return 4, nil
	})
	router := chi.NewRouter()
	handler.RegisterRoutes(router)
	put := func(path, body string, signedIn bool, status int) {
		t.Helper()
		recorder := httptest.NewRecorder()
		request := httptest.NewRequest(http.MethodPut, path, strings.NewReader(body))
		if signedIn {
			request.Header.Set("Authorization", "Bearer test")
		}
		router.ServeHTTP(recorder, request)
		if recorder.Code != status {
			t.Fatalf("%s = %d, want %d: %s", path, recorder.Code, status, recorder.Body.String())
		}
	}

	const workout = "/fitness-workouts/12/classification"
	const friday = "/fitness/workout-schedule/5"
	put(friday, `{"workout_type":"push"}`, false, http.StatusUnauthorized)
	put(workout, `{"workout_type":"pull","workout_focus":"  Back and biceps  "}`, true, http.StatusNoContent)
	put(friday, `{"workout_type":"push","workout_focus":"Chest and triceps"}`, true, http.StatusNoContent)
	put(friday, `{"workout_type":"rest_day","workout_focus":"Chest and triceps"}`, true, http.StatusNoContent)
	if workoutType := data.classification.WorkoutType; workoutType == nil || *workoutType != "pull" {
		t.Fatalf("workout type did not persist independently of the plan: %v", workoutType)
	}
	if focus := data.classification.WorkoutFocus; focus == nil || *focus != "Back and biceps" {
		t.Fatalf("workout focus was not saved and trimmed: %v", focus)
	}
	if day := data.days[5]; day.WorkoutType == nil || *day.WorkoutType != "rest_day" || day.WorkoutFocus != nil {
		t.Fatalf("expected a rest day without workout focus: %+v", day)
	}

	put(workout, `{"workout_type":"rest_day"}`, true, http.StatusBadRequest)
	put(workout, `{"workout_focus":"Missing type"}`, true, http.StatusBadRequest)
	put("/fitness-workouts/13/classification", `{"workout_type":"pull"}`, true, http.StatusNotFound)
	put("/fitness/workout-schedule/8", `{"workout_type":"full_body"}`, true, http.StatusBadRequest)
	put(friday, `{"workout_type":""}`, true, http.StatusNoContent)
	put(workout, `{"workout_type":""}`, true, http.StatusNoContent)
	if data.days[5].WorkoutType != nil || data.classification.WorkoutType != nil || data.classification.WorkoutFocus != nil {
		t.Fatal("expected the plan and workout classification to be cleared")
	}
}
