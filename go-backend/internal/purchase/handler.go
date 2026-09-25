package purchase

import (
	"database/sql"
	"encoding/json"
	"errors"
	"net/http"

	"github.com/go-chi/chi/v5"

	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type input struct {
	Name  string  `json:"name"`
	Price float64 `json:"price"`
	URL   *string `json:"url"`
}

type itemDTO struct {
	ID    int64   `json:"id"`
	Name  string  `json:"name"`
	Price float64 `json:"price"`
	URL   *string `json:"url"`
}

type Handler struct {
	service *Service
	userID  func(*http.Request) (int64, error)
}

func NewHandler(service *Service, userID func(*http.Request) (int64, error)) *Handler {
	return &Handler{service: service, userID: userID}
}

func (h *Handler) RegisterRoutes(r chi.Router) {
	r.Get("/next-month-purchases", h.ListNextMonth)
	r.Post("/next-month-purchases", h.CreateNextMonth)
	r.Delete("/next-month-purchases", h.ClearNextMonth)
	r.Delete("/next-month-purchases/{id}", h.DeleteNextMonth)
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

func toDTO(item Item) itemDTO {
	return itemDTO{ID: item.ID, Name: item.Name, Price: item.Price, URL: item.URL}
}

func (h *Handler) ListNextMonth(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	items, total, err := h.service.List(r.Context(), id)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	out := make([]itemDTO, 0, len(items))
	for _, item := range items {
		out = append(out, toDTO(item))
	}
	webutil.WriteJSON(w, http.StatusOK, map[string]any{"items": out, "total": total})
}

func (h *Handler) CreateNextMonth(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	var in input
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<20)).Decode(&in); err != nil {
		webutil.BadRequest(w, "invalid json")
		return
	}
	item, err := h.service.Create(r.Context(), id, in.Name, in.Price, in.URL)
	if errors.Is(err, ErrInvalidItem) {
		webutil.BadRequest(w, "name and a valid price are required")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusCreated, toDTO(item))
}

func (h *Handler) DeleteNextMonth(w http.ResponseWriter, r *http.Request) {
	id, ok := h.authenticatedUser(w, r)
	if !ok {
		return
	}
	purchaseID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	isDeleted, err := h.service.Delete(r.Context(), id, purchaseID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	if !isDeleted {
		http.NotFound(w, r)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) ClearNextMonth(w http.ResponseWriter, r *http.Request) {
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
