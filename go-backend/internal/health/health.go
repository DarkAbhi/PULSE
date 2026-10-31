// Package health exposes liveness and database readiness checks.
package health

import (
	"context"
	"net/http"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/DarkAbhi/life-backend/internal/webutil"
)

// Handler serves process and database health checks.
type Handler struct {
	DB *pgxpool.Pool
}

// New creates health checks backed by db.
func New(db *pgxpool.Pool) *Handler {
	return &Handler{DB: db}
}

// Liveness reports whether the API process can serve requests.
// @Summary Check liveness
// @Description Returns OK when the API process is running.
// @Tags health
// @Produce json
// @Success 200 {object} map[string]string
// @Router /healthz [get]
func (h *Handler) Liveness(w http.ResponseWriter, r *http.Request) {
	webutil.WriteJSON(w, http.StatusOK, map[string]string{"status": "ok"})
}

// Readyz reports whether the API can reach PostgreSQL within two seconds.
// @Summary Check readiness
// @Description Returns OK when the API can reach its database.
// @Tags health
// @Produce json
// @Success 200 {object} map[string]string
// @Failure 503 {object} map[string]string
// @Router /readyz [get]
func (h *Handler) Readyz(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
	defer cancel()
	if err := h.DB.Ping(ctx); err != nil {
		webutil.WriteJSON(w, http.StatusServiceUnavailable, map[string]string{"status": "db not ready"})
		return
	}
	webutil.WriteJSON(w, http.StatusOK, map[string]string{"status": "ready"})
}
