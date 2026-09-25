package auth

import (
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
