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
	r.Get("/meal-times", h.ListMealTimes)
	r.Post("/meal-times", h.CreateMealTime)
	r.Delete("/meal-times/{id}", h.DeleteMealTime)
	r.Get("/meal-plans", h.ListMealPlans)
	r.Post("/meal-plans", h.CreateMealPlan)
	r.Patch("/meal-plans/{id}/consumed", h.UpdateMealPlanConsumed)
	r.Patch("/meal-plans/{id}", h.UpdateMealPlan)
	r.Put("/meal-plans/{id}", h.UpdateMealPlan)
	r.Delete("/meal-plans/{id}", h.DeleteMealPlan)
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

func mealError(w http.ResponseWriter, err error) {
	var invalid ValidationError
	switch {
	case errors.As(err, &invalid):
		webutil.BadRequest(w, invalid.Message)
	case errors.Is(err, ErrDuplicateMealTime):
		webutil.BadRequest(w, "a meal time with this name already exists")
	case errors.Is(err, ErrMealTimeNotFound):
		webutil.BadRequest(w, "specified meal_time_id not found")
	case errors.Is(err, ErrMealNotFound):
		webutil.WriteJSON(w, http.StatusNotFound, map[string]string{"error": "meal not found"})
	default:
		webutil.ServerError(w, err)
	}
}

func (h *Handler) ListMealTimes(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	times, err := h.service.ListTimes(r.Context(), id)
	if err != nil {
		mealError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, times)
}

func (h *Handler) CreateMealTime(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	var in CreateMealTimeInput
	if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
		webutil.BadRequest(w, "invalid request body")
		return
	}
	item, err := h.service.CreateTime(r.Context(), id, in)
	if err != nil {
		mealError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusCreated, item)
}

func (h *Handler) DeleteMealTime(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	mealTimeID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	err := h.service.DeleteTime(r.Context(), id, mealTimeID)
	if errors.Is(err, ErrMealTimeNotFound) {
		webutil.WriteJSON(w, http.StatusNotFound, map[string]string{"error": "custom meal time not found or cannot be deleted"})
		return
	}
	if err != nil {
		mealError(w, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) ListMealPlans(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	q := r.URL.Query()
	items, err := h.service.ListPlans(r.Context(), id, q.Get("date"), q.Get("start_date"), q.Get("end_date"))
	if err != nil {
		mealError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, items)
}

func (h *Handler) CreateMealPlan(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	var in CreateMealPlanInput
	if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
		webutil.BadRequest(w, "invalid request body")
		return
	}
	item, err := h.service.CreatePlan(r.Context(), id, in)
	if err != nil {
		mealError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusCreated, item)
}

func (h *Handler) DeleteMealPlan(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	mealID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	if err := h.service.DeletePlan(r.Context(), id, mealID); err != nil {
		mealError(w, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) UpdateMealPlanConsumed(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	mealID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	var in UpdateMealPlanConsumedInput
	if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
		webutil.BadRequest(w, "invalid request body")
		return
	}
	item, err := h.service.SetConsumed(r.Context(), id, mealID, in.IsConsumed)
	if err != nil {
		mealError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, item)
}

func (h *Handler) UpdateMealPlan(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	mealID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	var in UpdateMealPlanInput
	if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
		webutil.BadRequest(w, "invalid request body")
		return
	}
	item, err := h.service.UpdatePlan(r.Context(), id, mealID, in)
	if err != nil {
		mealError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, item)
}
