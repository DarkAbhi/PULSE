package auth

import (
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestExtractSessionTokenPrecedence(t *testing.T) {
	for _, tc := range []struct{ name, authorization, cookie, want string }{
		{"bearer", "Bearer session-token", "", "session-token"},
		{"case insensitive", "bEaReR session-token", "", "session-token"},
		{"cookie compatibility", "", "cookie-token", "cookie-token"},
		{"bearer wins", "Bearer bearer-token", "cookie-token", "bearer-token"},
		{"invalid header does not fall back", "Basic credentials", "cookie-token", ""},
		{"empty bearer does not fall back", "Bearer ", "cookie-token", ""},
	} {
		t.Run(tc.name, func(t *testing.T) {
			req := httptest.NewRequest(http.MethodGet, "/", nil)
			req.Header.Set("Authorization", tc.authorization)
			if tc.cookie != "" {
				req.AddCookie(&http.Cookie{Name: SessionCookieName, Value: tc.cookie})
			}
			if got := ExtractSessionToken(req); got != tc.want {
				t.Fatalf("token = %q, want %q", got, tc.want)
			}
		})
	}
}
