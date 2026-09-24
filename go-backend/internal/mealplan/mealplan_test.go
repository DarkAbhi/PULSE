package mealplan

import (
	"bytes"
	"context"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/DarkAbhi/life-backend/internal/db/sqlc"
	"github.com/DarkAbhi/life-backend/internal/testhelper"
)

func TestMealPlanSQLC(t *testing.T) {
	db, cleanup := testhelper.StartPostgres(t)
	defer cleanup()
	ctx := context.Background()
	q := sqlc.New(db)
	created, err := q.CreateMealPlan(ctx, sqlc.CreateMealPlanParams{UserID: 1, Column2: "2026-09-21", Name: "Salad"})
	if err != nil {
		t.Fatal(err)
	}
	if created.Date != "2026-09-21" || created.StartTime != "" {
		t.Fatalf("unexpected created meal: %+v", created)
	}
	updated, err := q.SetMealPlanConsumed(ctx, sqlc.SetMealPlanConsumedParams{IsConsumed: true, ID: created.ID, UserID: 1})
	if err != nil || !updated.IsConsumed {
		t.Fatalf("set consumed: %+v, %v", updated, err)
	}
	changed, err := q.UpdateMealPlan(ctx, sqlc.UpdateMealPlanParams{Name: "Soup", Column2: "2026-09-22", IsConsumed: true, ID: created.ID, UserID: 1})
	if err != nil || changed.Name != "Soup" {
		t.Fatalf("update meal: %+v, %v", changed, err)
	}
	rows, err := q.ListMealPlansByDate(ctx, sqlc.ListMealPlansByDateParams{UserID: 1, Column2: "2026-09-22"})
	if err != nil || len(rows) != 1 || rows[0].ID != created.ID {
		t.Fatalf("list meals: %+v, %v", rows, err)
	}
}

func TestMealPlan_Unauthorized(t *testing.T) {
	h := NewHandler(nil)

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
