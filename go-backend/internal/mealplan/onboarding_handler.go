package mealplan

import (
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"slices"

	"github.com/DarkAbhi/life-backend/internal/webutil"
)

func decodePreferences(w http.ResponseWriter, r *http.Request, value any) bool {
	r.Body = http.MaxBytesReader(w, r.Body, 16*1024)
	decoder := json.NewDecoder(r.Body)
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(value); err != nil {
		webutil.BadRequest(w, "invalid preference request body")
		return false
	}
	if err := decoder.Decode(new(any)); err != io.EOF {
		webutil.BadRequest(w, "request must contain one JSON object")
		return false
	}
	return true
}

func writeOnboardingResult(w http.ResponseWriter, r *http.Request, state OnboardingState, err error) {
	w.Header().Set("Cache-Control", "no-store")
	// Older Android releases reject responses with a fourth meal kind.
	if state.Preferences != nil && r.URL.Query().Get("planning_slots") != "1" {
		preferences := *state.Preferences
		preferences.PlanningSlotsConfigured = false
		preferences.Meals = slices.DeleteFunc(slices.Clone(preferences.Meals), func(meal MealPreferenceTime) bool {
			return meal.Kind == "evening_snacks"
		})
		if len(preferences.Meals) == 0 {
			preferences.Meals = []MealPreferenceTime{{Kind: "breakfast", UsualTime: "09:00"}}
		}
		state.Preferences = &preferences
	}
	if errors.Is(err, ErrOnboardingConflict) || errors.Is(err, ErrOnboardingPending) {
		webutil.WriteJSON(w, http.StatusConflict, OnboardingConflictResponse{Error: err.Error(), State: state})
		return
	}
	if err != nil {
		writeError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, state)
}

// GetOnboarding returns the current account's decision and saved preferences.
// @Summary Read meal setup and preferences
// @Tags meal-plan
// @Produce json
// @Security SessionBearer
// @Param planning_slots query int false "Send 1 to include Evening Snacks and planning-slot settings"
// @Success 200 {object} OnboardingState
// @Failure 401 {object} apidoc.Error
// @Failure 500 {object} apidoc.Error
// @Router /api/meal-plan-onboarding [get]
func (h *Handler) GetOnboarding(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	state, err := h.service.GetOnboarding(r.Context(), id)
	writeOnboardingResult(w, r, state, err)
}

// ResolveOnboarding commits an explicit skip or a complete questionnaire atomically.
// @Summary Finish or skip meal setup
// @Description Version 1. First terminal decision wins; identical status retries return saved state. Conflicts return error and authoritative state.
// @Tags meal-plan
// @Accept json
// @Produce json
// @Security SessionBearer
// @Param body body OnboardingInput true "Explicit decision; completed requires preferences"
// @Param planning_slots query int false "Send 1 to include Evening Snacks and planning-slot settings"
// @Success 200 {object} OnboardingState
// @Failure 400 {object} apidoc.Error
// @Failure 401 {object} apidoc.Error
// @Failure 409 {object} OnboardingConflictResponse
// @Failure 500 {object} apidoc.Error
// @Router /api/meal-plan-onboarding [put]
func (h *Handler) ResolveOnboarding(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	var in OnboardingInput
	if !decodePreferences(w, r, &in) {
		return
	}
	state, err := h.service.ResolveOnboarding(r.Context(), id, in)
	writeOnboardingResult(w, r, state, err)
}

type OnboardingConflictResponse struct {
	Error string          `json:"error"`
	State OnboardingState `json:"state"`
}

// SavePreferences updates explicit settings without resetting onboarding.
// @Summary Update meal preferences after setup
// @Tags meal-plan
// @Accept json
// @Produce json
// @Security SessionBearer
// @Param body body MealPreferences true "Complete replacement preferences"
// @Param planning_slots query int false "Send 1 to include Evening Snacks and planning-slot settings"
// @Success 200 {object} OnboardingState
// @Failure 400 {object} apidoc.Error
// @Failure 401 {object} apidoc.Error
// @Failure 409 {object} OnboardingConflictResponse
// @Failure 500 {object} apidoc.Error
// @Router /api/meal-preferences [put]
func (h *Handler) SavePreferences(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	var in MealPreferences
	if !decodePreferences(w, r, &in) {
		return
	}
	state, err := h.service.SavePreferences(r.Context(), id, in)
	writeOnboardingResult(w, r, state, err)
}
