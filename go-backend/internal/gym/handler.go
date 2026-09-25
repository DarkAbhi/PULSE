package gym

import (
	"database/sql"
	"encoding/json"
	"errors"
	"net/http"
	"time"

	"github.com/go-chi/chi/v5"

	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type Handler struct {
	service *Service
	userID  func(*http.Request) (int64, error)
}

func NewHandler(service *Service, userID func(*http.Request) (int64, error)) *Handler {
	return &Handler{service: service, userID: userID}
}

func (h *Handler) RegisterRoutes(r chi.Router) {
	r.Get("/workout/today", h.GymVisitedToday)
	r.Post("/workout/today", h.AddWorkoutForDay)
	r.Get("/gym-visits", h.ListGymVisits)
	r.Delete("/gym-visits/{id}", h.DeleteGymVisit)
	r.Get("/gym-visits/{id}/exercises", h.GetGymVisitExercises)
	r.Post("/gym-visits/{id}/exercises", h.CreateGymVisitExercise)
	r.Post("/notifications/{id}/gym-visit", h.MarkGymReminderVisited)
}

func gymError(w http.ResponseWriter, r *http.Request, err error) {
	var invalid ValidationError
	switch {
	case errors.As(err, &invalid):
		webutil.BadRequest(w, invalid.Message)
	case errors.Is(err, ErrVisitNotFound), errors.Is(err, ErrReminderNotFound):
		http.NotFound(w, r)
	default:
		webutil.ServerError(w, err)
	}
}
func (h *Handler) GymVisitedToday(w http.ResponseWriter, r *http.Request) {
	id, visited, err := h.service.VisitedToday(r.Context(), time.Now())
	if err != nil {
		gymError(w, r, err)
		return
	}
	if !visited {
		webutil.WriteJSON(w, http.StatusOK, map[string]any{"visited": false})
		return
	}
	webutil.WriteJSON(w, http.StatusOK, map[string]any{"visited": true, "id": id})
}
func (h *Handler) AddWorkoutForDay(w http.ResponseWriter, r *http.Request) {
	id, err := h.service.AddVisit(r.Context())
	if err != nil {
		gymError(w, r, err)
		return
	}
	webutil.WriteJSON(w, http.StatusCreated, map[string]any{"message": "success", "id": id})
}
func (h *Handler) ListGymVisits(w http.ResponseWriter, r *http.Request) {
	items, err := h.service.ListVisits(r.Context())
	if err != nil {
		gymError(w, r, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, items)
}
func (h *Handler) DeleteGymVisit(w http.ResponseWriter, r *http.Request) {
	id, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	if err := h.service.DeleteVisit(r.Context(), id); err != nil {
		gymError(w, r, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
func (h *Handler) GetGymVisitExercises(w http.ResponseWriter, r *http.Request) {
	id, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	items, err := h.service.ListExercises(r.Context(), id)
	if err != nil {
		gymError(w, r, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, items)
}
func (h *Handler) CreateGymVisitExercise(w http.ResponseWriter, r *http.Request) {
	id, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20)
	defer r.Body.Close()
	var body createGymExerciseBody
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		webutil.BadRequest(w, "invalid JSON")
		return
	}
	item, err := h.service.CreateExercise(r.Context(), id, body)
	if err != nil {
		gymError(w, r, err)
		return
	}
	webutil.WriteJSON(w, http.StatusCreated, item)
}
func (h *Handler) MarkGymReminderVisited(w http.ResponseWriter, r *http.Request) {
	userID, err := h.userID(r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		gymError(w, r, err)
		return
	}
	id, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	visitID, err := h.service.MarkReminderVisited(r.Context(), userID, id)
	if err != nil {
		gymError(w, r, err)
		return
	}
	webutil.WriteJSON(w, http.StatusCreated, map[string]any{"id": visitID})
}
