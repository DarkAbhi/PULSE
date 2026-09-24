package profile

import (
	"database/sql"
	"encoding/json"
	"errors"
	"net/http"
	"strings"

	"golang.org/x/crypto/bcrypt"

	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/DarkAbhi/life-backend/internal/db/sqlc"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type profileBody struct {
	Name string `json:"name"`
}

type changePasswordBody struct {
	CurrentPassword string `json:"current_password"`
	NewPassword     string `json:"new_password"`
}

type Handler struct {
	DB *sql.DB
}

func NewHandler(db *sql.DB) *Handler {
	return &Handler{DB: db}
}

// GetProfile reports whether the signed-in user has completed first-run setup.
func (h *Handler) GetProfile(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	name, err := sqlc.New(h.DB).GetProfileName(r.Context(), user.ID)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.WriteJSON(w, http.StatusOK, map[string]any{"has_profile": false})
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, map[string]any{"has_profile": true, "name": name})
}

// SaveProfile creates or updates the signed-in user's profile.
func (h *Handler) SaveProfile(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	r.Body = http.MaxBytesReader(w, r.Body, 1<<20)
	defer r.Body.Close()
	var body profileBody
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		webutil.BadRequest(w, "invalid JSON")
		return
	}

	name := strings.TrimSpace(body.Name)
	if name == "" || len([]rune(name)) > 120 {
		webutil.BadRequest(w, "name must be between 1 and 120 characters")
		return
	}

	err = sqlc.New(h.DB).SaveProfile(r.Context(), sqlc.SaveProfileParams{UserID: user.ID, DisplayName: name})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, map[string]string{"name": name})
}

// ChangePassword updates the signed-in user's password.
func (h *Handler) ChangePassword(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	r.Body = http.MaxBytesReader(w, r.Body, 1<<20)
	defer r.Body.Close()
	var body changePasswordBody
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		webutil.BadRequest(w, "invalid JSON")
		return
	}

	if body.CurrentPassword == "" {
		webutil.BadRequest(w, "current password is required")
		return
	}
	if len(body.NewPassword) < 6 {
		webutil.BadRequest(w, "new password must be at least 6 characters")
		return
	}

	storedHash, err := sqlc.New(h.DB).GetPasswordHash(r.Context(), user.ID)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "user not found")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	if err := bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(body.CurrentPassword)); err != nil {
		webutil.BadRequest(w, "current password is incorrect")
		return
	}

	newHash, err := bcrypt.GenerateFromPassword([]byte(body.NewPassword), bcrypt.DefaultCost)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	err = sqlc.New(h.DB).UpdatePasswordHash(r.Context(), sqlc.UpdatePasswordHashParams{PasswordHash: string(newHash), ID: user.ID})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	webutil.WriteJSON(w, http.StatusOK, map[string]string{"message": "password updated successfully"})
}
