package mealplan

import (
	"database/sql"
	"encoding/json"
	"errors"
	"net/http"

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
	r.Get("/meal-times", h.ListTimes)
	r.Post("/meal-times", h.CreateTime)
	r.Delete("/meal-times/{id}", h.DeleteTime)
	r.Get("/meal-plans", h.ListPlans)
	r.Post("/meal-plans", h.CreatePlan)
	r.Patch("/meal-plans/{id}/consumed", h.UpdatePlanConsumed)
	r.Patch("/meal-plans/{id}", h.UpdatePlan)
	r.Put("/meal-plans/{id}", h.UpdatePlan)
	r.Delete("/meal-plans/{id}", h.DeletePlan)
}

func (h *Handler) authenticatedUser(w http.ResponseWriter, r *http.Request) (int64, bool) {
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

func writeError(w http.ResponseWriter, err error) {
	var invalid ValidationError
	switch {
	case errors.As(err, &invalid):
		webutil.BadRequest(w, invalid.Message)
	case errors.Is(err, ErrDuplicateTime):
		webutil.BadRequest(w, "a meal time with this name already exists")
	case errors.Is(err, ErrTimeNotFound):
		webutil.BadRequest(w, "specified meal time id not found")
	case errors.Is(err, ErrPlanNotFound):
		webutil.WriteJSON(w, http.StatusNotFound, map[string]string{"error": "meal not found"})
	default:
		webutil.ServerError(w, err)
	}
}

func (h *Handler) ListTimes(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	times, err := h.service.ListTimes(r.Context(), id)
	if err != nil {
		writeError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, times)
}

func (h *Handler) CreateTime(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	var in CreateTimeInput
	if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
		webutil.BadRequest(w, "invalid request body")
		return
	}
	item, err := h.service.CreateTime(r.Context(), id, in)
	if err != nil {
		writeError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusCreated, item)
}

func (h *Handler) DeleteTime(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	mealTimeID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	err := h.service.DeleteTime(r.Context(), id, mealTimeID)
	if errors.Is(err, ErrTimeNotFound) {
		webutil.WriteJSON(w, http.StatusNotFound, map[string]string{"error": "custom meal time not found or cannot be deleted"})
		return
	}
	if err != nil {
		writeError(w, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) ListPlans(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	q := r.URL.Query()
	items, err := h.service.ListPlans(r.Context(), id, q.Get("date"), q.Get("start_date"), q.Get("end_date"))
	if err != nil {
		writeError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, items)
}

func (h *Handler) CreatePlan(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	var in CreatePlanInput
	if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
		webutil.BadRequest(w, "invalid request body")
		return
	}
	item, err := h.service.CreatePlan(r.Context(), id, in)
	if err != nil {
		writeError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusCreated, item)
}

func (h *Handler) DeletePlan(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	mealID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	if err := h.service.DeletePlan(r.Context(), id, mealID); err != nil {
		writeError(w, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) UpdatePlanConsumed(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	mealID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	var in UpdatePlanConsumedInput
	if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
		webutil.BadRequest(w, "invalid request body")
		return
	}
	item, err := h.service.SetConsumed(r.Context(), id, mealID, in.IsConsumed)
	if err != nil {
		writeError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, item)
}

func (h *Handler) UpdatePlan(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	mealID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	var in UpdatePlanInput
	if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
		webutil.BadRequest(w, "invalid request body")
		return
	}
	item, err := h.service.UpdatePlan(r.Context(), id, mealID, in)
	if err != nil {
		writeError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, item)
}
