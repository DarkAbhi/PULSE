package mealplan

import (
	"bytes"
	"net/http"
	"net/http/httptest"
	"testing"
)

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
