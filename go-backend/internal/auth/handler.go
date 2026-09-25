package auth

import (
	"encoding/json"
	"errors"
	"net/http"
	"strings"
	"time"

	"github.com/go-chi/chi/v5"

	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type loginBody struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

type Handler struct {
	service *Service
	secure  bool
}

func NewHandler(service *Service, secure bool) *Handler {
	return &Handler{service: service, secure: secure}
}
func (h *Handler) RegisterRoutes(r chi.Router) {
	r.Post("/auth/login", h.Login)
	r.Get("/auth/session", h.Session)
	r.Post("/auth/logout", h.Logout)
}

func (h *Handler) Login(w http.ResponseWriter, r *http.Request) {
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20)
	defer r.Body.Close()
	var body loginBody
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		webutil.BadRequest(w, "invalid JSON")
		return
	}
	username := strings.TrimSpace(body.Username)
	if username == "" || body.Password == "" {
		webutil.BadRequest(w, "username and password are required")
		return
	}
	token, expires, err := h.service.Login(r.Context(), username, body.Password)
	if errors.Is(err, ErrInvalidCredentials) {
		webutil.Unauthorized(w, "invalid username or password")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	http.SetCookie(w, &http.Cookie{Name: SessionCookieName, Value: token, Path: "/", Expires: expires, HttpOnly: true, SameSite: http.SameSiteLaxMode, Secure: h.secure})
	webutil.WriteJSON(w, http.StatusOK, map[string]string{"username": username})
}
func (h *Handler) Session(w http.ResponseWriter, r *http.Request) {
	user, err := h.service.SessionUser(r.Context(), ExtractSessionToken(r))
	if errors.Is(err, ErrSessionNotFound) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, map[string]string{"username": user.Username})
}
func (h *Handler) Logout(w http.ResponseWriter, r *http.Request) {
	_ = h.service.Logout(r.Context(), ExtractSessionToken(r))
	http.SetCookie(w, &http.Cookie{Name: SessionCookieName, Value: "", Path: "/", Expires: time.Unix(0, 0), MaxAge: -1, HttpOnly: true, SameSite: http.SameSiteLaxMode, Secure: h.secure})
	webutil.WriteJSON(w, http.StatusOK, map[string]string{"message": "logged out"})
}
