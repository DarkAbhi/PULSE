package mealplan

import (
	"context"
	"errors"
	"fmt"
	"slices"
	"time"
)

const QuestionnaireVersion = 1

var ErrOnboardingConflict = errors.New("mealplan: onboarding was already resolved")
var ErrOnboardingPending = errors.New("mealplan: finish or skip setup before editing preferences")

// Codes are stable API values; UI labels may change without rewriting user data.
var goalCodes = []string{"feel_satisfied", "food_calm", "nourish_without_dieting"}
var dietCodes = []string{"no_preference", "vegetarian", "vegan", "pescatarian"}
var barrierCodes = []string{"hungry_again", "too_tired", "unsure_what_to_add", "food_advice_overwhelming"}
var avoidanceCodes = []string{"shellfish", "fish", "peanuts", "tree_nuts", "dairy", "eggs", "gluten", "soy", "sesame", "pork", "beef", "poultry", "mushrooms", "onion_garlic", "spicy_food", "alcohol"}
var mealKindCodes = []string{"breakfast", "lunch", "dinner"}

type MealPreferenceTime struct {
	Kind      string `json:"kind" enums:"breakfast,lunch,dinner"`
	UsualTime string `json:"usual_time" example:"09:00"`
}

type MealPreferences struct {
	Goal           string               `json:"goal" enums:"feel_satisfied,food_calm,nourish_without_dieting"`
	Barriers       []string             `json:"barriers"`
	DietaryPattern string               `json:"dietary_pattern" enums:"no_preference,vegetarian,vegan,pescatarian"`
	FoodAvoidances []string             `json:"food_avoidances"`
	Meals          []MealPreferenceTime `json:"meals"`
}

type OnboardingState struct {
	Status               string           `json:"status" enums:"pending,skipped,completed"`
	QuestionnaireVersion int32            `json:"questionnaire_version"`
	DecidedAt            *time.Time       `json:"decided_at,omitempty"`
	Preferences          *MealPreferences `json:"preferences"`
}

type OnboardingInput struct {
	Status               string           `json:"status" enums:"skipped,completed"`
	QuestionnaireVersion int32            `json:"questionnaire_version" example:"1"`
	Preferences          *MealPreferences `json:"preferences,omitempty"`
}

func validSelections(values, allowed []string) bool {
	seen := make(map[string]bool, len(values))
	for _, value := range values {
		if !slices.Contains(allowed, value) || seen[value] {
			return false
		}
		seen[value] = true
	}
	return true
}

func validatePreferences(p *MealPreferences) error {
	if p == nil {
		return ValidationError{"preferences are required"}
	}
	if !slices.Contains(goalCodes, p.Goal) {
		return ValidationError{"choose a valid goal"}
	}
	if !slices.Contains(dietCodes, p.DietaryPattern) {
		return ValidationError{"choose a valid dietary pattern"}
	}
	if p.Barriers == nil || !validSelections(p.Barriers, barrierCodes) {
		return ValidationError{"barriers must be an array of unique valid choices"}
	}
	if p.FoodAvoidances == nil || !validSelections(p.FoodAvoidances, avoidanceCodes) {
		return ValidationError{"food avoidances must be an array of unique valid choices"}
	}
	if len(p.Meals) < 1 || len(p.Meals) > 3 {
		return ValidationError{"choose at least one meal"}
	}
	kinds := make([]string, 0, len(p.Meals))
	for _, meal := range p.Meals {
		kinds = append(kinds, meal.Kind)
		parsed, err := time.Parse("15:04", meal.UsualTime)
		if err != nil || parsed.Format("15:04") != meal.UsualTime {
			return ValidationError{"meal times must use HH:MM"}
		}
	}
	if !validSelections(kinds, mealKindCodes) {
		return ValidationError{"meals must have unique valid kinds"}
	}
	return nil
}

func (s *Service) GetOnboarding(ctx context.Context, userID int64) (OnboardingState, error) {
	state, err := s.store.GetOnboarding(ctx, userID)
	if err != nil {
		return state, fmt.Errorf("get meal onboarding: %w", err)
	}
	return state, nil
}

func (s *Service) ResolveOnboarding(ctx context.Context, userID int64, in OnboardingInput) (OnboardingState, error) {
	if in.QuestionnaireVersion != QuestionnaireVersion {
		return OnboardingState{}, ValidationError{"unsupported questionnaire version"}
	}
	if in.Status != "skipped" && in.Status != "completed" {
		return OnboardingState{}, ValidationError{"status must be skipped or completed"}
	}
	if in.Status == "skipped" && in.Preferences != nil {
		return OnboardingState{}, ValidationError{"skip must not include preferences"}
	}
	if in.Status == "completed" {
		if err := validatePreferences(in.Preferences); err != nil {
			return OnboardingState{}, err
		}
	}
	return s.store.ResolveOnboarding(ctx, userID, in)
}

func (s *Service) SavePreferences(ctx context.Context, userID int64, in MealPreferences) (OnboardingState, error) {
	if err := validatePreferences(&in); err != nil {
		return OnboardingState{}, err
	}
	return s.store.SavePreferences(ctx, userID, in)
}
