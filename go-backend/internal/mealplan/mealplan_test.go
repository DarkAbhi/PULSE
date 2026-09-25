package mealplan

import (
	"bytes"
	"context"
	"database/sql"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strconv"
	"testing"

	"github.com/go-chi/chi/v5"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
)

func TestMealPlanSQLC(t *testing.T) {
	_, dsn, cleanup := testhelper.StartPostgresWithDSN(t)
	defer cleanup()
	ctx := context.Background()
	pool, err := pgxpool.New(ctx, dsn)
	if err != nil {
		t.Fatal(err)
	}
	defer pool.Close()
	q := NewRepository(pool)
	created, err := q.CreateMealPlan(ctx, CreateMealPlanParams{UserID: 1, Column2: "2026-09-21", Name: "Salad"})
	if err != nil {
		t.Fatal(err)
	}
	if created.Date != "2026-09-21" || created.StartTime != "" {
		t.Fatalf("unexpected created meal: %+v", created)
	}
	updated, err := q.SetMealPlanConsumed(ctx, SetMealPlanConsumedParams{IsConsumed: true, ID: created.ID, UserID: 1})
	if err != nil || !updated.IsConsumed {
		t.Fatalf("set consumed: %+v, %v", updated, err)
	}
	changed, err := q.UpdateMealPlan(ctx, UpdateMealPlanParams{Name: "Soup", Column2: "2026-09-22", IsConsumed: true, ID: created.ID, UserID: 1})
	if err != nil || changed.Name != "Soup" {
		t.Fatalf("update meal: %+v, %v", changed, err)
	}
	rows, err := q.ListMealPlansByDate(ctx, ListMealPlansByDateParams{UserID: 1, Column2: "2026-09-22"})
	if err != nil || len(rows) != 1 || rows[0].ID != created.ID {
		t.Fatalf("list meals: %+v, %v", rows, err)
	}
}

func TestMealPlan_Unauthorized(t *testing.T) {
	h := NewHandler(nil, func(*http.Request) (int64, error) { return 0, sql.ErrNoRows })

	t.Run("ListMealPlans without session", func(t *testing.T) {
		req := httptest.NewRequest(http.MethodGet, "/api/meal-plans", nil)
		rec := httptest.NewRecorder()
		h.ListMealPlans(rec, req)

		if rec.Code != http.StatusUnauthorized {
			t.Errorf("expected 401 Unauthorized, got %d", rec.Code)
		}
	})

	t.Run("CreateMealPlan without session", func(t *testing.T) {
		req := httptest.NewRequest(http.MethodPost, "/api/meal-plans", bytes.NewBufferString(`{"date":"2026-09-21","name":"Salad"}`))
		rec := httptest.NewRecorder()
		h.CreateMealPlan(rec, req)

		if rec.Code != http.StatusUnauthorized {
			t.Errorf("expected 401 Unauthorized, got %d", rec.Code)
		}
	})

	t.Run("DeleteMealPlan without session", func(t *testing.T) {
		req := httptest.NewRequest(http.MethodDelete, "/api/meal-plans/1", nil)
		rec := httptest.NewRecorder()
		h.DeleteMealPlan(rec, req)

		if rec.Code != http.StatusUnauthorized {
			t.Errorf("expected 401 Unauthorized, got %d", rec.Code)
		}
	})

	t.Run("UpdateMealPlanConsumed without session", func(t *testing.T) {
		req := httptest.NewRequest(http.MethodPatch, "/api/meal-plans/1/consumed", bytes.NewBufferString(`{"is_consumed":true}`))
		rec := httptest.NewRecorder()
		h.UpdateMealPlanConsumed(rec, req)

		if rec.Code != http.StatusUnauthorized {
			t.Errorf("expected 401 Unauthorized, got %d", rec.Code)
		}
	})

	t.Run("UpdateMealPlan without session", func(t *testing.T) {
		req := httptest.NewRequest(http.MethodPatch, "/api/meal-plans/1", bytes.NewBufferString(`{"name":"Updated Meal"}`))
		rec := httptest.NewRecorder()
		h.UpdateMealPlan(rec, req)

		if rec.Code != http.StatusUnauthorized {
			t.Errorf("expected 401 Unauthorized, got %d", rec.Code)
		}
	})

	t.Run("ListMealTimes without session", func(t *testing.T) {
		req := httptest.NewRequest(http.MethodGet, "/api/meal-times", nil)
		rec := httptest.NewRecorder()
		h.ListMealTimes(rec, req)

		if rec.Code != http.StatusUnauthorized {
			t.Errorf("expected 401 Unauthorized, got %d", rec.Code)
		}
	})

	t.Run("CreateMealTime without session", func(t *testing.T) {
		req := httptest.NewRequest(http.MethodPost, "/api/meal-times", bytes.NewBufferString(`{"name":"Snack","start_time":"15:00","end_time":"16:00"}`))
		rec := httptest.NewRecorder()
		h.CreateMealTime(rec, req)

		if rec.Code != http.StatusUnauthorized {
			t.Errorf("expected 401 Unauthorized, got %d", rec.Code)
		}
	})

	t.Run("DeleteMealTime without session", func(t *testing.T) {
		req := httptest.NewRequest(http.MethodDelete, "/api/meal-times/1", nil)
		rec := httptest.NewRecorder()
		h.DeleteMealTime(rec, req)

		if rec.Code != http.StatusUnauthorized {
			t.Errorf("expected 401 Unauthorized, got %d", rec.Code)
		}
	})
}

func TestParseTimeFlexible(t *testing.T) {
	tests := []struct {
		input   string
		valid   bool
		expTime string
	}{
		{"07:00", true, "07:00:00"},
		{"14:30", true, "14:30:00"},
		{"19:30:00", true, "19:30:00"},
		{"25:00", false, ""},
		{"invalid", false, ""},
		{"", false, ""},
	}

	for _, tc := range tests {
		got, err := parseTimeFlexible(tc.input)
		if tc.valid && err != nil {
			t.Errorf("expected %q to be valid, got err: %v", tc.input, err)
		}
		if !tc.valid && err == nil {
			t.Errorf("expected %q to fail validation, got %q", tc.input, got)
		}
		if tc.valid && got != tc.expTime {
			t.Errorf("expected %q for input %q, got %q", tc.expTime, tc.input, got)
		}
	}
}

func TestMealPlanRoutes(t *testing.T) {
	_, dsn, stop := testhelper.StartPostgresWithDSN(t)
	defer stop()
	pool, err := pgxpool.New(context.Background(), dsn)
	if err != nil {
		t.Fatal(err)
	}
	defer pool.Close()
	h := NewHandler(NewService(NewRepository(pool)), func(*http.Request) (int64, error) { return 1, nil })
	rec := httptest.NewRecorder()
	h.CreateMealPlan(rec, httptest.NewRequest(http.MethodPost, "/api/meal-plans", bytes.NewBufferString(`{"date":"2026-09-21","name":"Salad"}`)))
	if rec.Code != http.StatusCreated {
		t.Fatalf("create: %d %s", rec.Code, rec.Body.String())
	}
	var meal MealPlanDTO
	if err := json.NewDecoder(rec.Body).Decode(&meal); err != nil {
		t.Fatal(err)
	}
	rec = httptest.NewRecorder()
	h.UpdateMealPlanConsumed(rec, requestWithID(http.MethodPatch, "/api/meal-plans/1/consumed", `{"is_consumed":true}`, meal.ID))
	if rec.Code != http.StatusOK {
		t.Fatalf("consume: %d %s", rec.Code, rec.Body.String())
	}
	rec = httptest.NewRecorder()
	h.ListMealPlans(rec, httptest.NewRequest(http.MethodGet, "/api/meal-plans?date=2026-09-21", nil))
	if rec.Code != http.StatusOK {
		t.Fatalf("list: %d %s", rec.Code, rec.Body.String())
	}
	var meals []MealPlanDTO
	if err := json.NewDecoder(rec.Body).Decode(&meals); err != nil || len(meals) != 1 || !meals[0].IsConsumed {
		t.Fatalf("list: %+v %v", meals, err)
	}
}

func requestWithID(method, path, body string, id int64) *http.Request {
	r := httptest.NewRequest(method, path, bytes.NewBufferString(body))
	route := chi.NewRouteContext()
	route.URLParams.Add("id", strconv.FormatInt(id, 10))
	return r.WithContext(context.WithValue(r.Context(), chi.RouteCtxKey, route))
}
