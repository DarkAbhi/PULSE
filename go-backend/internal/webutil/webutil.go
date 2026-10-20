// Package webutil provides shared JSON response and request parsing helpers.
package webutil

import (
	"context"
	"encoding/json"
	"log/slog"
	"net/http"
	"strconv"

	"github.com/go-chi/chi/v5"
)

// WriteJSON writes v as a JSON response with status.
func WriteJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}

// BadRequest writes a 400 JSON error response.
func BadRequest(w http.ResponseWriter, msg string) {
	WriteJSON(w, http.StatusBadRequest, map[string]string{"error": msg})
}

// Unauthorized writes a 401 JSON error response.
func Unauthorized(w http.ResponseWriter, msg string) {
	WriteJSON(w, http.StatusUnauthorized, map[string]string{"error": msg})
}

// ServerError logs err and writes a generic 500 JSON error response.
func ServerError(w http.ResponseWriter, err error) {
	slog.Error("request failed", "error", err)
	WriteJSON(w, http.StatusInternalServerError, map[string]string{"error": "internal server error"})
}

// ServerErrorContext logs err with ctx and writes a generic 500 JSON error response.
func ServerErrorContext(ctx context.Context, w http.ResponseWriter, err error) {
	slog.ErrorContext(ctx, "request failed", "error", err)
	WriteJSON(w, http.StatusInternalServerError, map[string]string{"error": "internal server error"})
}

// ParseID reads a positive integer "id" route parameter.
// It writes a 400 response and returns false when the parameter is invalid.
func ParseID(w http.ResponseWriter, r *http.Request) (int64, bool) {
	idStr := chi.URLParam(r, "id")
	id, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil || id <= 0 {
		BadRequest(w, "invalid id")
		return 0, false
	}
	return id, true
}
