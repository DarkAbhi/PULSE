package gym

import (
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"strconv"

	"github.com/go-chi/chi/v5"

	"github.com/DarkAbhi/life-backend/internal/webutil"
)

func readWorkoutClassification(w http.ResponseWriter, r *http.Request) (string, string, bool) {
	var body struct {
		WorkoutType  *string `json:"workout_type"`
		WorkoutFocus string  `json:"workout_focus"`
	}
	decoder := json.NewDecoder(http.MaxBytesReader(w, r.Body, 4096))
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(&body); err != nil || !errors.Is(decoder.Decode(new(json.RawMessage)), io.EOF) {
		webutil.BadRequest(w, "invalid json")
		return "", "", false
	}
	if body.WorkoutType == nil {
		webutil.BadRequest(w, "workout_type is required")
		return "", "", false
	}
	return *body.WorkoutType, body.WorkoutFocus, true
}

func (h *Handler) UpdateWorkoutClassification(w http.ResponseWriter, r *http.Request) {
	userID, ok := h.sessionUserID(w, r)
	if !ok {
		return
	}
	visitID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	workoutType, focus, ok := readWorkoutClassification(w, r)
	if !ok {
		return
	}
	if err := h.service.UpdateWorkoutClassification(r.Context(), userID, visitID, workoutType, focus); err != nil {
		writeError(w, r, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) ListWorkoutSchedule(w http.ResponseWriter, r *http.Request) {
	userID, ok := h.sessionUserID(w, r)
	if !ok {
		return
	}
	days, err := h.service.ListWorkoutSchedule(r.Context(), userID)
	if err != nil {
		writeError(w, r, err)
		return
	}
	w.Header().Set("Cache-Control", "no-store")
	webutil.WriteJSON(w, http.StatusOK, days)
}

func (h *Handler) SaveWorkoutScheduleDay(w http.ResponseWriter, r *http.Request) {
	userID, ok := h.sessionUserID(w, r)
	if !ok {
		return
	}
	weekday, err := strconv.ParseInt(chi.URLParam(r, "weekday"), 10, 16)
	if err != nil {
		webutil.BadRequest(w, "invalid weekday")
		return
	}
	workoutType, focus, ok := readWorkoutClassification(w, r)
	if !ok {
		return
	}
	if err := h.service.SaveWorkoutScheduleDay(r.Context(), userID, int16(weekday), workoutType, focus); err != nil {
		writeError(w, r, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
