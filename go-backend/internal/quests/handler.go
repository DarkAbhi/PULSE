package quests

import (
	"database/sql"
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"time"

	"github.com/go-chi/chi/v5"

	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type Handler struct {
	repository *Repository
	userID     func(*http.Request) (int64, error)
}

func NewHandler(repository *Repository, userID func(*http.Request) (int64, error)) *Handler {
	return &Handler{repository: repository, userID: userID}
}

func (h *Handler) RegisterRoutes(r chi.Router) {
	r.Get("/quests/overview", h.authenticated(h.overview))
	r.Get("/quests/history", h.authenticated(h.history))
	r.Get("/quests/settings", h.authenticated(h.settings))
	r.Put("/quests/settings", h.authenticated(h.saveSettings))
}

func (h *Handler) authenticated(next func(http.ResponseWriter, *http.Request, int64)) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Cache-Control", "no-store")
		userID, err := h.userID(r)
		if errors.Is(err, sql.ErrNoRows) {
			webutil.Unauthorized(w, "session is invalid or expired")
			return
		}
		if err != nil {
			webutil.ServerError(w, err)
			return
		}
		next(w, r, userID)
	}
}

func (h *Handler) overview(w http.ResponseWriter, r *http.Request, userID int64) {
	var week time.Time
	if raw := r.URL.Query().Get("week"); raw != "" {
		var err error
		week, err = time.Parse(time.DateOnly, raw)
		if err != nil {
			webutil.BadRequest(w, "Enter the week date as YYYY-MM-DD.")
			return
		}
	}
	result, err := h.repository.Overview(r.Context(), userID, week)
	if err != nil {
		webutil.ServerErrorContext(r.Context(), w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, result)
}

func (h *Handler) settings(w http.ResponseWriter, r *http.Request, userID int64) {
	settings, err := h.repository.Settings(r.Context(), userID)
	if err != nil {
		webutil.ServerErrorContext(r.Context(), w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, settings)
}

func (h *Handler) history(w http.ResponseWriter, r *http.Request, userID int64) {
	params := r.URL.Query()
	from, fromErr := time.Parse(time.DateOnly, params.Get("from"))
	to, toErr := time.Parse(time.DateOnly, params.Get("to"))
	if fromErr != nil || toErr != nil || from.After(to) || from.Year() < 1 {
		webutil.BadRequest(w, "Enter an ordered date range using from and to in YYYY-MM-DD format.")
		return
	}
	history, err := h.repository.History(r.Context(), userID, from, to)
	if err != nil {
		webutil.ServerErrorContext(r.Context(), w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, history)
}

func (h *Handler) saveSettings(w http.ResponseWriter, r *http.Request, userID int64) {
	var settings *Settings
	decoder := json.NewDecoder(http.MaxBytesReader(w, r.Body, 4096))
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(&settings); err != nil || settings == nil || decoder.Decode(new(any)) != io.EOF {
		webutil.BadRequest(w, "Invalid quest settings.")
		return
	}
	if message := settings.validationError(); message != "" {
		webutil.BadRequest(w, message)
		return
	}
	effective, err := h.repository.SaveSettings(r.Context(), userID, *settings)
	if err != nil {
		webutil.ServerErrorContext(r.Context(), w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, map[string]string{"effective_date": effective})
}
