package profile

import (
	"encoding/json"
	"errors"
	"net/http"

	"github.com/DarkAbhi/life-backend/internal/webutil"
)

func (h *Handler) ListAPIKeys(w http.ResponseWriter, r *http.Request) {
	userID, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	keys, err := h.service.ListAPIKeys(r.Context(), userID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	w.Header().Set("Cache-Control", "no-store")
	webutil.WriteJSON(w, http.StatusOK, keys)
}

func (h *Handler) CreateAPIKey(w http.ResponseWriter, r *http.Request) {
	userID, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20)
	defer r.Body.Close()
	var body struct {
		Name string `json:"name"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		webutil.BadRequest(w, "invalid json")
		return
	}
	key, token, err := h.service.CreateAPIKey(r.Context(), userID, body.Name)
	if errors.Is(err, ErrInvalidAPIKeyName) {
		webutil.BadRequest(w, "name must be between 1 and 120 characters")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	w.Header().Set("Cache-Control", "no-store")
	webutil.WriteJSON(w, http.StatusCreated, struct {
		APIKey
		Token string `json:"token"`
	}{APIKey: key, Token: token})
}

func (h *Handler) RevokeAPIKey(w http.ResponseWriter, r *http.Request) {
	userID, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	keyID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	revoked, err := h.service.RevokeAPIKey(r.Context(), userID, keyID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	if !revoked {
		http.NotFound(w, r)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
