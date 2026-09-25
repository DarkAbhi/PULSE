package profile

import (
	"database/sql"
	"encoding/json"
	"errors"
	"net/http"

	"github.com/go-chi/chi/v5"

	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type Handler struct {
	service *Service
	userID  func(*http.Request) (int64, error)
}

func NewHandler(service *Service, userID func(*http.Request) (int64, error)) *Handler {
	return &Handler{service: service, userID: userID}
}
func (h *Handler) RegisterRoutes(r chi.Router) {
	r.Get("/profile", h.Show)
	r.Put("/profile", h.Save)
	r.Put("/profile/password", h.ChangePassword)
}
func (h *Handler) authenticatedUser(w http.ResponseWriter, r *http.Request) (int64, bool) {
	id, err := h.userID(r)
	if errors.Is(err, sql.ErrNoRows) || errors.Is(err, auth.ErrSessionNotFound) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return 0, false
	}
	if err != nil {
		webutil.ServerError(w, err)
		return 0, false
	}
	return id, true
}
func (h *Handler) Show(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	name, has, err := h.service.Fetch(r.Context(), id)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	if !has {
		webutil.WriteJSON(w, http.StatusOK, map[string]any{"has_profile": false})
		return
	}
	webutil.WriteJSON(w, http.StatusOK, map[string]any{"has_profile": true, "name": name})
}
func (h *Handler) Save(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20)
	defer r.Body.Close()
	var body saveBody
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		webutil.BadRequest(w, "invalid json")
		return
	}
	name, err := h.service.Save(r.Context(), id, body.Name)
	if errors.Is(err, ErrInvalidName) {
		webutil.BadRequest(w, "name must be between 1 and 120 characters")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, map[string]string{"name": name})
}
func (h *Handler) ChangePassword(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20)
	defer r.Body.Close()
	var body changePasswordBody
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		webutil.BadRequest(w, "invalid json")
		return
	}
	err := h.service.ChangePassword(r.Context(), id, body.CurrentPassword, body.NewPassword)
	switch {
	case errors.Is(err, ErrCurrentPasswordRequired):
		webutil.BadRequest(w, "current password is required")
	case errors.Is(err, ErrNewPasswordTooShort):
		webutil.BadRequest(w, "new password must be at least 6 characters")
	case errors.Is(err, auth.ErrUserNotFound):
		webutil.Unauthorized(w, "user not found")
	case errors.Is(err, auth.ErrWrongPassword):
		webutil.BadRequest(w, "current password is incorrect")
	case err != nil:
		webutil.ServerError(w, err)
	default:
		webutil.WriteJSON(w, http.StatusOK, map[string]string{"message": "password updated successfully"})
	}
}
