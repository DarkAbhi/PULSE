package webutil

import (
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/go-chi/chi/v5"
)

func TestWriteJSON(t *testing.T) {
	rec := httptest.NewRecorder()
	payload := map[string]string{"message": "hello"}

	WriteJSON(rec, http.StatusAccepted, payload)

	if rec.Code != http.StatusAccepted {
		t.Errorf("expected status %d, got %d", http.StatusAccepted, rec.Code)
	}

	if contentType := rec.Header().Get("Content-Type"); contentType != "application/json" {
		t.Errorf("expected Content-Type application/json, got %q", contentType)
	}

	var out map[string]string
	if err := json.NewDecoder(rec.Body).Decode(&out); err != nil {
		t.Fatalf("failed to decode body: %v", err)
	}

	if out["message"] != "hello" {
		t.Errorf("expected message hello, got %q", out["message"])
	}
}

func TestBadRequest(t *testing.T) {
	rec := httptest.NewRecorder()
	BadRequest(rec, "some bad request error")

	if rec.Code != http.StatusBadRequest {
		t.Errorf("expected status 400, got %d", rec.Code)
	}

	var out map[string]string
	_ = json.NewDecoder(rec.Body).Decode(&out)
	if out["error"] != "some bad request error" {
		t.Errorf("expected error field to match, got %v", out)
	}
}

func TestUnauthorized(t *testing.T) {
	rec := httptest.NewRecorder()
	Unauthorized(rec, "session expired")

	if rec.Code != http.StatusUnauthorized {
		t.Errorf("expected status 401, got %d", rec.Code)
	}

	var out map[string]string
	_ = json.NewDecoder(rec.Body).Decode(&out)
	if out["error"] != "session expired" {
		t.Errorf("expected error field to match, got %v", out)
	}
}

func TestServerError(t *testing.T) {
	rec := httptest.NewRecorder()
	ServerError(rec, errors.New("db failure"))

	if rec.Code != http.StatusInternalServerError {
		t.Errorf("expected status 500, got %d", rec.Code)
	}

	var out map[string]string
	_ = json.NewDecoder(rec.Body).Decode(&out)
	if out["error"] != "internal server error" {
		t.Errorf("expected generic error, got %v", out)
	}
}

func TestParseID(t *testing.T) {
	tests := []struct {
		name         string
		paramValue   string
		expectedID   int64
		expectedOK   bool
		expectedCode int
	}{
		{"valid positive id", "123", 123, true, http.StatusOK},
		{"invalid string", "abc", 0, false, http.StatusBadRequest},
		{"negative number", "-1", 0, false, http.StatusBadRequest},
		{"zero number", "0", 0, false, http.StatusBadRequest},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			rec := httptest.NewRecorder()
			req := httptest.NewRequest(http.MethodGet, "/{id}", nil)

			// Setup chi context
			rctx := chi.NewRouteContext()
			rctx.URLParams.Add("id", tt.paramValue)
			req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

			id, ok := ParseID(rec, req)
			if ok != tt.expectedOK {
				t.Errorf("ParseID() ok = %v, wantOk = %v", ok, tt.expectedOK)
			}
			if id != tt.expectedID {
				t.Errorf("ParseID() id = %v, wantID = %v", id, tt.expectedID)
			}
			if !tt.expectedOK && rec.Code != tt.expectedCode {
				t.Errorf("expected status code %d, got %d", tt.expectedCode, rec.Code)
			}
		})
	}
}
