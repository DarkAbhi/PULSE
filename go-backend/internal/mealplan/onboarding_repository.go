package mealplan

import (
	"context"
	"errors"
	"fmt"

	"github.com/DarkAbhi/life-backend/internal/mealplan/query"
	"github.com/jackc/pgx/v5"
)

func readOnboarding(ctx context.Context, q *query.Queries, userID int64) (OnboardingState, error) {
	state := OnboardingState{Status: "pending", QuestionnaireVersion: QuestionnaireVersion}
	row, err := q.GetMealOnboarding(ctx, userID)
	if errors.Is(err, pgx.ErrNoRows) {
		return state, nil
	}
	if err != nil {
		return state, err
	}
	state.Status, state.QuestionnaireVersion, state.DecidedAt = row.Status, row.QuestionnaireVersion, &row.DecidedAt.Time
	p, err := q.GetMealPreferences(ctx, userID)
	if errors.Is(err, pgx.ErrNoRows) {
		if state.Status == "completed" {
			return state, errors.New("completed onboarding has no preferences")
		}
		return state, nil
	}
	if err != nil {
		return state, err
	}
	times, err := q.ListMealPreferenceTimes(ctx, userID)
	if err != nil {
		return state, err
	}
	state.Preferences = &MealPreferences{Goal: p.Goal, DietaryPattern: p.DietaryPattern, Barriers: p.Barriers, FoodAvoidances: p.FoodAvoidances, Meals: make([]MealPreferenceTime, 0, len(times))}
	for _, t := range times {
		state.Preferences.Meals = append(state.Preferences.Meals, MealPreferenceTime{Kind: t.MealKind, UsualTime: t.UsualTime})
	}
	return state, nil
}

// Serialize reads and mutations per account, including the first write when no
// onboarding row exists yet. Other accounts remain independent.
func (r *Repository) withOnboardingLock(ctx context.Context, userID int64, fn func(*query.Queries) (OnboardingState, error)) (OnboardingState, error) {
	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return OnboardingState{}, err
	}
	defer tx.Rollback(ctx)
	q := r.queries.WithTx(tx)
	if _, err = q.LockMealPreferenceUser(ctx, userID); err != nil {
		return OnboardingState{}, fmt.Errorf("lock meal preference user: %w", err)
	}
	state, err := fn(q)
	if err != nil {
		return state, err
	}
	if err = tx.Commit(ctx); err != nil {
		return OnboardingState{}, err
	}
	return state, nil
}

func (r *Repository) GetOnboarding(ctx context.Context, userID int64) (OnboardingState, error) {
	return r.withOnboardingLock(ctx, userID, func(q *query.Queries) (OnboardingState, error) { return readOnboarding(ctx, q, userID) })
}

func writePreferences(ctx context.Context, q *query.Queries, userID int64, p MealPreferences) error {
	if err := q.UpsertMealPreferences(ctx, query.UpsertMealPreferencesParams{UserID: userID, Goal: p.Goal, DietaryPattern: p.DietaryPattern, Barriers: p.Barriers, FoodAvoidances: p.FoodAvoidances}); err != nil {
		return err
	}
	if err := q.DeleteMealPreferenceTimes(ctx, userID); err != nil {
		return err
	}
	for _, meal := range p.Meals {
		if err := q.CreateMealPreferenceTime(ctx, query.CreateMealPreferenceTimeParams{UserID: userID, MealKind: meal.Kind, UsualTime: meal.UsualTime}); err != nil {
			return err
		}
	}
	return nil
}

func (r *Repository) ResolveOnboarding(ctx context.Context, userID int64, in OnboardingInput) (OnboardingState, error) {
	return r.withOnboardingLock(ctx, userID, func(q *query.Queries) (OnboardingState, error) {
		state, err := readOnboarding(ctx, q, userID)
		if err != nil {
			return state, err
		}
		if state.Status != "pending" {
			if state.Status != in.Status {
				return state, ErrOnboardingConflict
			}
			return state, nil
		}
		if err = q.CreateMealOnboarding(ctx, query.CreateMealOnboardingParams{UserID: userID, Status: in.Status, QuestionnaireVersion: in.QuestionnaireVersion}); err != nil {
			return state, err
		}
		if in.Preferences != nil {
			if err = writePreferences(ctx, q, userID, *in.Preferences); err != nil {
				return state, err
			}
		}
		return readOnboarding(ctx, q, userID)
	})
}

func (r *Repository) SavePreferences(ctx context.Context, userID int64, p MealPreferences) (OnboardingState, error) {
	return r.withOnboardingLock(ctx, userID, func(q *query.Queries) (OnboardingState, error) {
		state, err := readOnboarding(ctx, q, userID)
		if err != nil {
			return state, err
		}
		if state.Status == "pending" {
			return state, ErrOnboardingPending
		}
		if err = writePreferences(ctx, q, userID, p); err != nil {
			return state, err
		}
		return readOnboarding(ctx, q, userID)
	})
}
