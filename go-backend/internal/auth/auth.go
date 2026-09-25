package auth

import (
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"net/http"
	"strings"
	"time"
)

const (
	SessionCookieName = "life_session"
	SessionLifetime   = 30 * 24 * time.Hour
)

type SessionUser struct {
	ID       int64
	Username string
}

// GetSessionUser supports SQL-backed integration tests.
func GetSessionUser(db *sql.DB, r *http.Request) (SessionUser, error) {
	token := ExtractSessionToken(r)
	if token == "" {
		return SessionUser{}, sql.ErrNoRows
	}
	hash := sha256.Sum256([]byte(token))
	var user SessionUser
	err := db.QueryRowContext(r.Context(), `SELECT users.id, users.username FROM user_sessions JOIN users ON users.id=user_sessions.user_id WHERE user_sessions.token_hash=$1 AND user_sessions.expires_at>NOW()`, hex.EncodeToString(hash[:])).Scan(&user.ID, &user.Username)
	return user, err
}

func ExtractSessionToken(r *http.Request) string {
	if authHeader := r.Header.Get("Authorization"); authHeader != "" {
		if strings.HasPrefix(strings.ToLower(authHeader), "bearer ") {
			return strings.TrimSpace(authHeader[7:])
		}
	}
	if token := r.Header.Get("X-Session-Token"); token != "" {
		return strings.TrimSpace(token)
	}
	if cookie, err := r.Cookie(SessionCookieName); err == nil && cookie.Value != "" {
		return cookie.Value
	}
	return ""
}
