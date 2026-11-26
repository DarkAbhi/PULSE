package activity

import (
	"database/sql"
	"encoding/json"
	"errors"
	"net/http"
	"time"

	"github.com/go-chi/chi/v5"

	"github.com/DarkAbhi/life-backend/internal/activity/query"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type Handler struct {
	queries *query.Queries
	userID  func(*http.Request) (int64, error)
}

type Entry struct {
	ID         string          `json:"id"`
	Kind       string          `json:"kind"`
	RecordedAt time.Time       `json:"recorded_at"`
	Details    json.RawMessage `json:"details" swaggertype:"object"`
}

func NewHandler(db query.DBTX, userID func(*http.Request) (int64, error)) *Handler {
	return &Handler{queries: query.New(db), userID: userID}
}

func (h *Handler) RegisterRoutes(r chi.Router) {
	r.Get("/activity/recent", h.List)
}

// List documents GET /api/activity/recent.
// @Summary List the three most recent activity entries for the signed-in user
// @Tags activity
// @Produce json
// @Security SessionBearer
// @Success 200 {array} Entry
// @Failure 401 {object} apidoc.Error
// @Failure 500 {object} apidoc.Error
// @Router /api/activity/recent [get]
func (h *Handler) List(w http.ResponseWriter, r *http.Request) {
	userID, err := h.userID(r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	rows, err := h.queries.ListRecent(r.Context(), userID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	entries := make([]Entry, 0, len(rows))
	for _, row := range rows {
		entries = append(entries, Entry{
			ID: row.ID, Kind: row.Kind, RecordedAt: row.RecordedAt.Time.UTC(), Details: row.Details,
		})
	}
	w.Header().Set("Cache-Control", "no-store")
	webutil.WriteJSON(w, http.StatusOK, entries)
}
