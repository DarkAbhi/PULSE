package testhelper

import (
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"net/http"
	"strings"
)

type SessionUser struct {
	ID       int64
	Username string
}

// GetSessionUser supports integration tests that seed sessions with database/sql.
func GetSessionUser(db *sql.DB, r *http.Request) (SessionUser, error) {
	token := ""
	if header := r.Header.Get("Authorization"); strings.HasPrefix(strings.ToLower(header), "bearer ") {
		token = strings.TrimSpace(header[7:])
	} else if value := r.Header.Get("X-Session-Token"); value != "" {
		token = strings.TrimSpace(value)
	} else if cookie, err := r.Cookie("life_session"); err == nil {
		token = cookie.Value
	}
	if token == "" {
		return SessionUser{}, sql.ErrNoRows
	}
	hash := sha256.Sum256([]byte(token))
	var user SessionUser
	err := db.QueryRowContext(r.Context(), `SELECT users.id,users.username FROM user_sessions JOIN users ON users.id=user_sessions.user_id WHERE user_sessions.token_hash=$1 AND user_sessions.expires_at>NOW()`, hex.EncodeToString(hash[:])).Scan(&user.ID, &user.Username)
	return user, err
}
