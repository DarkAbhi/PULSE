package observability

import (
	"crypto/subtle"
	"net/http"
	"net/http/pprof"

	"github.com/go-chi/chi/v5"
)

// RegisterPprofRoutes mounts pprof debugging endpoints on the given chi router.
// If PprofEnabled is false, the endpoints are not mounted.
// If PprofAuthUser and PprofAuthPass are configured, endpoints are protected with HTTP Basic Auth.
func RegisterPprofRoutes(r chi.Router, cfg Config) {
	if !cfg.IsPprofEnabled {
		return
	}

	r.Route("/debug/pprof", func(pr chi.Router) {
		if cfg.PprofAuthUser != "" && cfg.PprofAuthPass != "" {
			pr.Use(basicAuthMiddleware(cfg.PprofAuthUser, cfg.PprofAuthPass))
		}

		pr.Get("/", pprof.Index)
		pr.Get("/cmdline", pprof.Cmdline)
		pr.Get("/profile", pprof.Profile)
		pr.Get("/symbol", pprof.Symbol)
		pr.Post("/symbol", pprof.Symbol)
		pr.Get("/trace", pprof.Trace)
		pr.Get("/goroutine", pprof.Handler("goroutine").ServeHTTP)
		pr.Get("/heap", pprof.Handler("heap").ServeHTTP)
		pr.Get("/threadcreate", pprof.Handler("threadcreate").ServeHTTP)
		pr.Get("/block", pprof.Handler("block").ServeHTTP)
		pr.Get("/mutex", pprof.Handler("mutex").ServeHTTP)
		pr.Get("/allocs", pprof.Handler("allocs").ServeHTTP)
	})
}

func basicAuthMiddleware(expectedUser, expectedPass string) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			user, pass, ok := r.BasicAuth()
			if !ok || subtle.ConstantTimeCompare([]byte(user), []byte(expectedUser)) != 1 ||
				subtle.ConstantTimeCompare([]byte(pass), []byte(expectedPass)) != 1 {
				w.Header().Set("WWW-Authenticate", `Basic realm="pprof"`)
				http.Error(w, "unauthorized", http.StatusUnauthorized)
				return
			}
			next.ServeHTTP(w, r)
		})
	}
}
