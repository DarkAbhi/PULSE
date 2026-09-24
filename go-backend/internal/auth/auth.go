package auth

import (
	"crypto/rand"
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"encoding/json"
	"errors"
	"net/http"
	"os"
	"strings"
	"time"

	"golang.org/x/crypto/bcrypt"

	"github.com/DarkAbhi/life-backend/internal/db/sqlc"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

const (
	SessionCookieName = "life_session"
	SessionLifetime   = 30 * 24 * time.Hour
)

type loginBody struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

type SessionUser struct {
	ID       int64
	Username string
}

type Handler struct {
	DB *sql.DB
}

func NewHandler(db *sql.DB) *Handler {
	return &Handler{DB: db}
}

// Login verifies a username and password and creates a database-backed session.
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

	loginUser, err := sqlc.New(h.DB).GetLoginUser(r.Context(), username)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "invalid username or password")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	if bcrypt.CompareHashAndPassword([]byte(loginUser.PasswordHash), []byte(body.Password)) != nil {
		webutil.Unauthorized(w, "invalid username or password")
		return
	}

	token, err := newSessionToken()
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	expiresAt := time.Now().UTC().Add(SessionLifetime)
	hash := sha256.Sum256([]byte(token))
	if err := sqlc.New(h.DB).CreateSession(r.Context(), sqlc.CreateSessionParams{
		UserID: loginUser.ID, TokenHash: hex.EncodeToString(hash[:]), ExpiresAt: expiresAt,
	}); err != nil {
		webutil.ServerError(w, err)
		return
	}

	http.SetCookie(w, &http.Cookie{
		Name:     SessionCookieName,
		Value:    token,
		Path:     "/",
		Expires:  expiresAt,
		HttpOnly: true,
		SameSite: http.SameSiteLaxMode,
		Secure:   os.Getenv("APP_ENV") == "production",
	})
	webutil.WriteJSON(w, http.StatusOK, map[string]string{"username": username})
}

// Session returns the signed-in user for a valid, non-expired session cookie.
func (h *Handler) Session(w http.ResponseWriter, r *http.Request) {
	user, err := GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, map[string]string{"username": user.Username})
}

// Logout deletes the database session and clears the cookie.
func (h *Handler) Logout(w http.ResponseWriter, r *http.Request) {
	cookie, err := r.Cookie(SessionCookieName)
	if err == nil && cookie.Value != "" {
		hash := sha256.Sum256([]byte(cookie.Value))
		_ = sqlc.New(h.DB).DeleteSession(r.Context(), hex.EncodeToString(hash[:]))
	}

	http.SetCookie(w, &http.Cookie{
		Name:     SessionCookieName,
		Value:    "",
		Path:     "/",
		Expires:  time.Unix(0, 0),
		MaxAge:   -1,
		HttpOnly: true,
		SameSite: http.SameSiteLaxMode,
		Secure:   os.Getenv("APP_ENV") == "production",
	})
	webutil.WriteJSON(w, http.StatusOK, map[string]string{"message": "logged out"})
}

// GetSessionUser retrieves the session user using cookie or HTTP authorization headers.
func GetSessionUser(db *sql.DB, r *http.Request) (SessionUser, error) {
	token := ExtractSessionToken(r)
	if token == "" {
		return SessionUser{}, sql.ErrNoRows
	}

	hash := sha256.Sum256([]byte(token))
	user, err := sqlc.New(db).GetSessionUser(r.Context(), hex.EncodeToString(hash[:]))
	return SessionUser{ID: user.ID, Username: user.Username}, err
}

func ExtractSessionToken(r *http.Request) string {
	if authHeader := r.Header.Get("Authorization"); authHeader != "" {
		if strings.HasPrefix(strings.ToLower(authHeader), "bearer ") {
			return strings.TrimSpace(authHeader[7:])
		}
	}
	if headerToken := r.Header.Get("X-Session-Token"); headerToken != "" {
		return strings.TrimSpace(headerToken)
	}
	if cookie, err := r.Cookie(SessionCookieName); err == nil && cookie.Value != "" {
		return cookie.Value
	}
	return ""
}

func newSessionToken() (string, error) {
	bytes := make([]byte, 32)
	if _, err := rand.Read(bytes); err != nil {
		return "", err
	}
	return hex.EncodeToString(bytes), nil
}
