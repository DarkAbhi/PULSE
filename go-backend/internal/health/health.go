package health

import (
	"context"
	"net/http"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type Handler struct {
	DB *pgxpool.Pool
}

func NewHandler(db *pgxpool.Pool) *Handler {
	return &Handler{DB: db}
}

func (h *Handler) Healthz(w http.ResponseWriter, r *http.Request) {
	webutil.WriteJSON(w, http.StatusOK, map[string]string{"status": "ok"})
}

func (h *Handler) Readyz(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
	defer cancel()
	if err := h.DB.Ping(ctx); err != nil {
		webutil.WriteJSON(w, http.StatusServiceUnavailable, map[string]string{"status": "db not ready", "error": err.Error()})
		return
	}
	webutil.WriteJSON(w, http.StatusOK, map[string]string{"status": "ready"})
}
