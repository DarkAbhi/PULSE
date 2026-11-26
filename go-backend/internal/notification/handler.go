package notification

import (
	"database/sql"
	"errors"
	"net/http"
	"strconv"

	"github.com/go-chi/chi/v5"

	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type itemDTO struct {
	ID         int64   `json:"id"`
	Source     string  `json:"source"`
	Title      string  `json:"title"`
	Body       *string `json:"body"`
	TargetPath *string `json:"target_path"`
	Priority   int     `json:"priority"`
	CreatedAt  string  `json:"created_at"`
}

type Handler struct {
	service *Service
	userID  func(*http.Request) (int64, error)
}

func NewHandler(service *Service, userID func(*http.Request) (int64, error)) *Handler {
	return &Handler{service: service, userID: userID}
}

func (h *Handler) RegisterRoutes(r chi.Router) {
	r.Get("/notifications", h.List)
	r.Delete("/notifications", h.Clear)
	r.Delete("/notifications/{id}", h.Dismiss)
}

func (h *Handler) authenticatedUser(w http.ResponseWriter, r *http.Request) (int64, bool) {
	id, err := h.userID(r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return 0, false
	}
	if err != nil {
		webutil.ServerError(w, err)
		return 0, false
	}
	return id, true
}

func (h *Handler) List(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	limit := 100
	if raw := r.URL.Query().Get("limit"); raw != "" {
		n, err := strconv.Atoi(raw)
		if err != nil || n < 1 || n > 100 {
			webutil.BadRequest(w, "limit must be between 1 and 100")
			return
		}
		limit = n
	}
	items, err := h.service.List(r.Context(), id, limit)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	out := make([]itemDTO, 0, len(items))
	for _, item := range items {
		out = append(out, itemDTO{ID: item.ID, Source: item.Source, Title: item.Title, Body: item.Body, TargetPath: item.TargetPath, Priority: int(item.Priority), CreatedAt: item.CreatedAt.UTC().Format("2006-01-02T15:04:05Z")})
	}
	webutil.WriteJSON(w, http.StatusOK, out)
}

func (h *Handler) Dismiss(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	notificationID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	isUpdated, err := h.service.Dismiss(r.Context(), id, notificationID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	if !isUpdated {
		http.NotFound(w, r)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) Clear(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	if err := h.service.Clear(r.Context(), id); err != nil {
		webutil.ServerError(w, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
