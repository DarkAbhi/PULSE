package activity

import (
	"encoding/json"
	"errors"
	"net/http"
	"time"

	"github.com/go-chi/chi/v5"

	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type Handler struct{ service *Service }

func NewHandler(service *Service) *Handler { return &Handler{service: service} }

func (h *Handler) RegisterRoutes(r chi.Router) {
	r.Post("/meditation/today", h.addMeditation)
	r.Post("/sport/today", h.addSport)
}

func (h *Handler) addMeditation(w http.ResponseWriter, r *http.Request) {
	err := h.service.AddMeditation(r.Context(), time.Now())
	if errors.Is(err, ErrAlreadyMeditated) {
		webutil.BadRequest(w, "You have already meditated today.")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusCreated, map[string]string{"message": "success"})
}

func (h *Handler) addSport(w http.ResponseWriter, r *http.Request) {
	var body struct {
		Sport string `json:"sport"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		webutil.BadRequest(w, "Invalid JSON.")
		return
	}
	err := h.service.AddSport(r.Context(), body.Sport)
	if errors.Is(err, ErrInvalidSport) {
		webutil.BadRequest(w, "This sport is not available yet.")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusCreated, map[string]string{"message": "success"})
}
