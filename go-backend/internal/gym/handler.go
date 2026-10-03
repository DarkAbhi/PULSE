package gym

import (
	"database/sql"
	"encoding/json"
	"errors"
	"net/http"
	"strconv"
	"time"

	"github.com/go-chi/chi/v5"

	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type Handler struct {
	service *Service
	userID  func(*http.Request) (int64, error)
}

func (h *Handler) sessionUserID(w http.ResponseWriter, r *http.Request) (int64, bool) {
	id, err := h.userID(r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return 0, false
	}
	if err != nil {
		webutil.ServerError(w, err)
		return 0, false
	}
	return id, true
}

func NewHandler(service *Service, userID func(*http.Request) (int64, error)) *Handler {
	return &Handler{service: service, userID: userID}
}

func (h *Handler) RegisterRoutes(r chi.Router) {
	r.Get("/exercise-catalog", h.SearchExerciseCatalog)
	r.Get("/fitness/overview", h.Overview)
	r.Get("/workout/today", h.VisitedToday)
	r.Post("/workout/today", h.AddWorkoutForDay)
	r.Get("/gym-visits", h.ListVisits)
	r.Post("/gym-visits", h.AddVisitOnDate)
	r.Get("/fitness-workouts/{id}", h.GetWorkout)
	r.Delete("/gym-visits/{id}", h.DeleteVisit)
	r.Get("/gym-visits/{id}/exercises", h.ListVisitExercises)
	r.Post("/gym-visits/{id}/exercises", h.CreateVisitExercise)
	r.Post("/gym-visits/{id}/exercises/batch", h.CreateVisitExercises)
	r.Delete("/gym-visits/{id}/exercises/{exerciseID}", h.DeleteVisitExercise)
}

func (h *Handler) SearchExerciseCatalog(w http.ResponseWriter, r *http.Request) {
	userID, ok := h.sessionUserID(w, r)
	if !ok {
		return
	}
	result, err := h.service.SearchCatalog(r.Context(), userID, r.URL.Query().Get("q"))
	if err != nil {
		writeError(w, r, err)
		return
	}
	w.Header().Set("Cache-Control", "no-store")
	webutil.WriteJSON(w, http.StatusOK, result)
}

func (h *Handler) Overview(w http.ResponseWriter, r *http.Request) {
	userID, ok := h.sessionUserID(w, r)
	if !ok {
		return
	}
	result, err := h.service.Overview(r.Context(), userID, time.Now())
	if err != nil {
		writeError(w, r, err)
		return
	}
	w.Header().Set("Cache-Control", "no-store")
	webutil.WriteJSON(w, http.StatusOK, result)
}

func writeError(w http.ResponseWriter, r *http.Request, err error) {
	var invalid ValidationError
	switch {
	case errors.As(err, &invalid):
		webutil.BadRequest(w, invalid.Message)
	case errors.Is(err, ErrVisitNotFound), errors.Is(err, ErrExerciseNotFound):
		http.NotFound(w, r)
	default:
		webutil.ServerError(w, err)
	}
}
func (h *Handler) VisitedToday(w http.ResponseWriter, r *http.Request) {
	userID, ok := h.sessionUserID(w, r)
	if !ok {
		return
	}
	id, visited, err := h.service.VisitedToday(r.Context(), userID, time.Now())
	if err != nil {
		writeError(w, r, err)
		return
	}
	if !visited {
		webutil.WriteJSON(w, http.StatusOK, map[string]any{"visited": false})
		return
	}
	webutil.WriteJSON(w, http.StatusOK, map[string]any{"visited": true, "id": id})
}
func (h *Handler) AddWorkoutForDay(w http.ResponseWriter, r *http.Request) {
	userID, ok := h.sessionUserID(w, r)
	if !ok {
		return
	}
	id, err := h.service.AddVisit(r.Context(), userID)
	if err != nil {
		writeError(w, r, err)
		return
	}
	webutil.WriteJSON(w, http.StatusCreated, map[string]any{"message": "success", "id": id})
}
func (h *Handler) AddVisitOnDate(w http.ResponseWriter, r *http.Request) {
	userID, ok := h.sessionUserID(w, r)
	if !ok {
		return
	}
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20)
	defer r.Body.Close()
	var body struct {
		Date string `json:"date"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		webutil.BadRequest(w, "invalid json")
		return
	}
	id, err := h.service.AddVisitOnDate(r.Context(), userID, body.Date)
	if err != nil {
		writeError(w, r, err)
		return
	}
	webutil.WriteJSON(w, http.StatusCreated, map[string]any{"message": "success", "id": id})
}

func (h *Handler) ListVisits(w http.ResponseWriter, r *http.Request) {
	userID, ok := h.sessionUserID(w, r)
	if !ok {
		return
	}
	items, err := h.service.ListVisits(r.Context(), userID)
	if err != nil {
		writeError(w, r, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, items)
}
func (h *Handler) GetWorkout(w http.ResponseWriter, r *http.Request) {
	userID, ok := h.sessionUserID(w, r)
	if !ok {
		return
	}
	id, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	item, err := h.service.GetVisit(r.Context(), userID, id)
	if err != nil {
		writeError(w, r, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, item)
}

func (h *Handler) DeleteVisit(w http.ResponseWriter, r *http.Request) {
	userID, ok := h.sessionUserID(w, r)
	if !ok {
		return
	}
	id, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	if err := h.service.DeleteVisit(r.Context(), userID, id); err != nil {
		writeError(w, r, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
func (h *Handler) DeleteVisitExercise(w http.ResponseWriter, r *http.Request) {
	userID, ok := h.sessionUserID(w, r)
	if !ok {
		return
	}
	visitID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	exerciseID, err := strconv.ParseInt(chi.URLParam(r, "exerciseID"), 10, 64)
	if err != nil || exerciseID <= 0 {
		webutil.BadRequest(w, "invalid exercise id")
		return
	}
	if err := h.service.DeleteExercise(r.Context(), userID, visitID, exerciseID); err != nil {
		writeError(w, r, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) ListVisitExercises(w http.ResponseWriter, r *http.Request) {
	userID, ok := h.sessionUserID(w, r)
	if !ok {
		return
	}
	id, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	items, err := h.service.ListExercises(r.Context(), userID, id)
	if err != nil {
		writeError(w, r, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, items)
}
func (h *Handler) CreateVisitExercise(w http.ResponseWriter, r *http.Request) {
	userID, ok := h.sessionUserID(w, r)
	if !ok {
		return
	}
	id, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20)
	defer r.Body.Close()
	var body createExerciseBody
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		webutil.BadRequest(w, "invalid json")
		return
	}
	item, err := h.service.CreateExercise(r.Context(), userID, id, body)
	if err != nil {
		writeError(w, r, err)
		return
	}
	webutil.WriteJSON(w, http.StatusCreated, item)
}

func (h *Handler) CreateVisitExercises(w http.ResponseWriter, r *http.Request) {
	userID, ok := h.sessionUserID(w, r)
	if !ok {
		return
	}
	id, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20)
	defer r.Body.Close()
	var body createExercisesBody
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		webutil.BadRequest(w, "invalid json")
		return
	}
	items, err := h.service.CreateExercises(r.Context(), userID, id, body.Exercises)
	if err != nil {
		writeError(w, r, err)
		return
	}
	webutil.WriteJSON(w, http.StatusCreated, items)
}
