package mealplan

import (
	"bytes"
	"context"
	"database/sql"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"reflect"
	"strings"
	"sync"
	"testing"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"github.com/go-chi/chi/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

func validMealPreferences() MealPreferences {
	return MealPreferences{Goal: "feel_satisfied", DietaryPattern: "vegetarian", Barriers: []string{}, FoodAvoidances: []string{}, Meals: []MealPreferenceTime{{Kind: "breakfast", UsualTime: "09:00"}, {Kind: "lunch", UsualTime: "12:30"}, {Kind: "dinner", UsualTime: "19:30"}}}
}

func TestMealPreferenceValidation(t *testing.T) {
	for _, goal := range goalCodes {
		for _, diet := range dietCodes {
			p := validMealPreferences()
			p.Goal = goal
			p.DietaryPattern = diet
			p.Barriers = append([]string{}, barrierCodes...)
			p.FoodAvoidances = append([]string{}, avoidanceCodes...)
			if err := validatePreferences(&p); err != nil {
				t.Fatalf("valid codes: %v", err)
			}
		}
	}
	cases := []struct {
		name   string
		change func(*MealPreferences)
	}{
		{"missing goal", func(p *MealPreferences) { p.Goal = "" }},
		{"unknown diet", func(p *MealPreferences) { p.DietaryPattern = "keto" }},
		{"null barriers", func(p *MealPreferences) { p.Barriers = nil }},
		{"unknown barrier", func(p *MealPreferences) { p.Barriers = []string{"unknown"} }},
		{"duplicate barrier", func(p *MealPreferences) { p.Barriers = []string{"too_tired", "too_tired"} }},
		{"null avoidances", func(p *MealPreferences) { p.FoodAvoidances = nil }},
		{"unknown avoidance", func(p *MealPreferences) { p.FoodAvoidances = []string{"unknown"} }},
		{"duplicate avoidance", func(p *MealPreferences) { p.FoodAvoidances = []string{"fish", "fish"} }},
		{"no meals", func(p *MealPreferences) { p.Meals = nil }},
		{"duplicate meal", func(p *MealPreferences) { p.Meals[1].Kind = "breakfast" }},
		{"unknown meal", func(p *MealPreferences) { p.Meals[0].Kind = "snack" }},
		{"invalid hour", func(p *MealPreferences) { p.Meals[0].UsualTime = "24:00" }},
		{"seconds", func(p *MealPreferences) { p.Meals[0].UsualTime = "09:00:00" }},
		{"short time", func(p *MealPreferences) { p.Meals[0].UsualTime = "9:00" }},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			p := validMealPreferences()
			tc.change(&p)
			var validation ValidationError
			if err := validatePreferences(&p); !errors.As(err, &validation) {
				t.Fatalf("expected validation error, got %v", err)
			}
		})
	}
	if err := validatePreferences(nil); err == nil {
		t.Fatal("nil preferences accepted")
	}
	for _, kind := range mealKindCodes {
		p := validMealPreferences()
		p.Meals = []MealPreferenceTime{{Kind: kind, UsualTime: "00:00"}}
		if err := validatePreferences(&p); err != nil {
			t.Fatal(err)
		}
	}
}

func TestOnboardingRejectsInvalidDecisionBeforeWriting(t *testing.T) {
	s := NewService(nil)
	p := validMealPreferences()
	for _, in := range []OnboardingInput{
		{Status: "pending", QuestionnaireVersion: 1}, {Status: "skipped", QuestionnaireVersion: 2},
		{Status: "completed", QuestionnaireVersion: 1}, {Status: "skipped", QuestionnaireVersion: 1, Preferences: &p},
	} {
		if _, err := s.ResolveOnboarding(context.Background(), 1, in); err == nil {
			t.Fatalf("accepted %+v", in)
		}
	}
}

func TestOnboardingHandlerAuthenticationAndStrictJSON(t *testing.T) {
	for _, methodPath := range [][2]string{{"GET", "/meal-plan-onboarding"}, {"PUT", "/meal-plan-onboarding"}, {"PUT", "/meal-preferences"}} {
		r := chi.NewRouter()
		NewHandler(nil, func(*http.Request) (int64, error) { return 0, sql.ErrNoRows }).RegisterRoutes(r)
		w := httptest.NewRecorder()
		r.ServeHTTP(w, httptest.NewRequest(methodPath[0], methodPath[1], nil))
		if w.Code != 401 {
			t.Fatalf("%v got %d", methodPath, w.Code)
		}
	}
	r := chi.NewRouter()
	NewHandler(NewService(nil), func(*http.Request) (int64, error) { return 1, nil }).RegisterRoutes(r)
	for _, body := range []string{`null`, `{}`, `{"status":"skipped","questionnaire_version":1,"user_id":2}`, `{"status":"skipped","questionnaire_version":1} {}`, `{"status":"skipped","questionnaire_version":2}`, strings.Repeat(" ", 17000) + `{}`} {
		w := httptest.NewRecorder()
		r.ServeHTTP(w, httptest.NewRequest("PUT", "/meal-plan-onboarding", strings.NewReader(body)))
		if w.Code != 400 {
			t.Fatalf("invalid body got %d", w.Code)
		}
	}
}

func TestMealOnboardingPersistence(t *testing.T) {
	database, dsn, stop := testhelper.StartPostgresWithDSN(t)
	defer stop()
	pool, err := pgxpool.New(context.Background(), dsn)
	if err != nil {
		t.Fatal(err)
	}
	defer pool.Close()
	repo := NewRepository(pool)
	service := NewService(repo)
	ctx := context.Background()
	newUser := func(name string) int64 {
		t.Helper()
		var id int64
		if err := database.QueryRow(`INSERT INTO users (username,password_hash) VALUES ($1,'test') RETURNING id`, name).Scan(&id); err != nil {
			t.Fatal(err)
		}
		return id
	}
	first, second := newUser("meal-setup-one"), newUser("meal-setup-two")
	state, err := service.GetOnboarding(ctx, first)
	if err != nil || state.Status != "pending" || state.Preferences != nil {
		t.Fatalf("fresh: %+v %v", state, err)
	}
	var count int
	if err := database.QueryRow(`SELECT count(*) FROM meal_plan_onboarding`).Scan(&count); err != nil || count != 0 {
		t.Fatalf("read created state: %d %v", count, err)
	}
	p := validMealPreferences()
	p.Barriers = append([]string{}, barrierCodes...)
	p.FoodAvoidances = append([]string{}, avoidanceCodes...)
	state, err = service.ResolveOnboarding(ctx, first, OnboardingInput{Status: "completed", QuestionnaireVersion: 1, Preferences: &p})
	if err != nil || state.Status != "completed" || state.DecidedAt == nil || !reflect.DeepEqual(state.Preferences, &p) {
		t.Fatalf("save: %+v %v", state, err)
	}
	stamp := *state.DecidedAt
	p.Goal = "food_calm"
	state, err = service.ResolveOnboarding(ctx, first, OnboardingInput{Status: "completed", QuestionnaireVersion: 1, Preferences: &p})
	if err != nil || state.Preferences.Goal != "feel_satisfied" || !stamp.Equal(*state.DecidedAt) {
		t.Fatalf("retry overwrote: %+v %v", state, err)
	}
	state, err = service.ResolveOnboarding(ctx, first, OnboardingInput{Status: "skipped", QuestionnaireVersion: 1})
	if !errors.Is(err, ErrOnboardingConflict) || state.Status != "completed" {
		t.Fatalf("conflict: %+v %v", state, err)
	}
	state, err = service.GetOnboarding(ctx, second)
	if err != nil || state.Status != "pending" || state.Preferences != nil {
		t.Fatalf("user isolation: %+v %v", state, err)
	}
	if _, err := service.SavePreferences(ctx, second, p); !errors.Is(err, ErrOnboardingPending) {
		t.Fatalf("pending settings save: %v", err)
	}
	state, err = service.ResolveOnboarding(ctx, second, OnboardingInput{Status: "skipped", QuestionnaireVersion: 1})
	if err != nil || state.Status != "skipped" || state.Preferences != nil {
		t.Fatalf("skip: %+v %v", state, err)
	}
	state, err = service.SavePreferences(ctx, second, p)
	if err != nil || state.Status != "skipped" || !reflect.DeepEqual(state.Preferences, &p) {
		t.Fatalf("settings after skip: %+v %v", state, err)
	}
	p.Meals = []MealPreferenceTime{{Kind: "dinner", UsualTime: "23:59"}}
	state, err = service.SavePreferences(ctx, first, p)
	if err != nil || !reflect.DeepEqual(state.Preferences, &p) || !stamp.Equal(*state.DecidedAt) {
		t.Fatalf("replace settings: %+v %v", state, err)
	}

	t.Run("HTTP returns authoritative saved state", func(t *testing.T) {
		r := chi.NewRouter()
		NewHandler(service, func(*http.Request) (int64, error) { return first, nil }).RegisterRoutes(r)
		for _, call := range []struct {
			method, path, body string
			code               int
		}{
			{"GET", "/meal-plan-onboarding", "", 200}, {"PUT", "/meal-plan-onboarding", `{"status":"skipped","questionnaire_version":1}`, 409},
		} {
			w := httptest.NewRecorder()
			r.ServeHTTP(w, httptest.NewRequest(call.method, call.path, bytes.NewBufferString(call.body)))
			if w.Code != call.code || w.Header().Get("Cache-Control") != "no-store" {
				t.Fatalf("HTTP: %d %s", w.Code, w.Body.String())
			}
			if !strings.Contains(w.Body.String(), `"completed"`) {
				t.Fatal("missing authoritative state")
			}
		}
		body, _ := json.Marshal(p)
		w := httptest.NewRecorder()
		r.ServeHTTP(w, httptest.NewRequest("PUT", "/meal-preferences", bytes.NewReader(body)))
		if w.Code != 200 {
			t.Fatalf("HTTP edit: %d %s", w.Code, w.Body.String())
		}
	})

	t.Run("database failure rolls back terminal decision and preferences", func(t *testing.T) {
		id := newUser("meal-rollback")
		invalid := validMealPreferences()
		invalid.Meals[1].UsualTime = "25:00"
		if _, err := repo.ResolveOnboarding(ctx, id, OnboardingInput{Status: "completed", QuestionnaireVersion: 1, Preferences: &invalid}); err == nil {
			t.Fatal("expected database failure")
		}
		state, err := repo.GetOnboarding(ctx, id)
		if err != nil || state.Status != "pending" {
			t.Fatalf("partial decision: %+v %v", state, err)
		}
		if err := database.QueryRow(`SELECT count(*) FROM meal_preferences WHERE user_id=$1`, id).Scan(&count); err != nil || count != 0 {
			t.Fatalf("partial preferences: %d %v", count, err)
		}
	})
	t.Run("concurrent first terminal decisions", func(t *testing.T) {
		id := newUser("meal-race")
		start := make(chan struct{})
		results := make(chan error, 2)
		var wg sync.WaitGroup
		for _, status := range []string{"skipped", "completed"} {
			wg.Add(1)
			go func(status string) {
				defer wg.Done()
				<-start
				in := OnboardingInput{Status: status, QuestionnaireVersion: 1}
				valid := validMealPreferences()
				if status == "completed" {
					in.Preferences = &valid
				}
				_, err := service.ResolveOnboarding(ctx, id, in)
				results <- err
			}(status)
		}
		close(start)
		wg.Wait()
		close(results)
		successes, conflicts := 0, 0
		for err := range results {
			if err == nil {
				successes++
			} else if errors.Is(err, ErrOnboardingConflict) {
				conflicts++
			} else {
				t.Fatal(err)
			}
		}
		if successes != 1 || conflicts != 1 {
			t.Fatalf("race: %d successes %d conflicts", successes, conflicts)
		}
		state, err := service.GetOnboarding(ctx, id)
		if err != nil || (state.Status == "completed") != (state.Preferences != nil) {
			t.Fatalf("inconsistent race state: %+v %v", state, err)
		}
	})
	if _, err := database.Exec(`DELETE FROM users WHERE id=$1`, first); err != nil {
		t.Fatal(err)
	}
	for _, table := range []string{"meal_plan_onboarding", "meal_preferences", "meal_preference_times"} {
		if err := database.QueryRow(`SELECT count(*) FROM `+table+` WHERE user_id=$1`, first).Scan(&count); err != nil || count != 0 {
			t.Fatalf("cascade %s: %d %v", table, count, err)
		}
	}
}
