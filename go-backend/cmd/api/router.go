package main

import (
	"net/http"
	"strings"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/swaggo/http-swagger/v2"

	"github.com/DarkAbhi/life-backend/internal/activity"
	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/DarkAbhi/life-backend/internal/gym"
	"github.com/DarkAbhi/life-backend/internal/health"
	"github.com/DarkAbhi/life-backend/internal/horizon"
	"github.com/DarkAbhi/life-backend/internal/mealplan"
	"github.com/DarkAbhi/life-backend/internal/notification"
	"github.com/DarkAbhi/life-backend/internal/observability"
	"github.com/DarkAbhi/life-backend/internal/profile"
	"github.com/DarkAbhi/life-backend/internal/purchase"
	"github.com/DarkAbhi/life-backend/internal/vehicle"
)

// API holds the dependencies used to build the HTTP handler.
type API struct {
	DB             *pgxpool.Pool
	AllowedOrigins []string
	Activity       *activity.Handler
	Purchase       *purchase.Handler
	MealPlan       *mealplan.Handler
	Notification   *notification.Handler
	Gym            *gym.Handler
	Vehicle        *vehicle.Handler
	Horizon        *horizon.Handler
	Auth           *auth.Handler
	Profile        *profile.Handler
	ObsConfig      observability.Config
}

// Router builds the API handler, including operational and feature routes.
// It panics when DB is nil because health and readiness checks require it.
func (a *API) Router() http.Handler {
	if a.DB == nil {
		panic("api DB is nil")
	}
	r := chi.NewRouter()

	serviceName := a.ObsConfig.ServiceName
	if serviceName == "" {
		serviceName = "life-backend"
	}

	r.Use(middleware.RequestID)
	r.Use(middleware.RealIP)
	r.Use(observability.HTTPMiddleware(serviceName))
	r.Use(middleware.Recoverer)
	r.Use(cors(a.AllowedOrigins))
	r.Use(middleware.Timeout(15 * time.Second))

	healthHandler := health.New(a.DB)

	// Health (outside /api so Docker or Kubernetes health probes stay simple)
	r.Get("/healthz", healthHandler.Liveness) // liveness
	r.Get("/readyz", healthHandler.Readyz)    // readiness (DB ping)

	// Prometheus Metrics & pprof Profiling
	r.Handle("/metrics", observability.MetricsHandler())
	observability.RegisterPprofRoutes(r, a.ObsConfig)

	r.Get("/swagger/*", httpSwagger.Handler())

	// All application APIs under /api
	r.Route("/api", func(api chi.Router) {
		if a.Auth != nil {
			a.Auth.RegisterRoutes(api)
		}
		if a.Profile != nil {
			a.Profile.RegisterRoutes(api)
		}
		if a.Notification != nil {
			a.Notification.RegisterRoutes(api)
		}
		if a.Purchase != nil {
			a.Purchase.RegisterRoutes(api)
		}

		if a.Horizon != nil {
			a.Horizon.RegisterRoutes(api)
		}

		// Daily logs
		if a.Gym != nil {
			a.Gym.RegisterRoutes(api)
		}
		if a.Activity != nil {
			a.Activity.RegisterRoutes(api)
		}

		// Meal plans & meal times
		if a.MealPlan != nil {
			a.MealPlan.RegisterRoutes(api)
		}

		if a.Vehicle != nil {
			a.Vehicle.RegisterRoutes(api)
		}

	})

	return r
}

// cors permits the separately hosted Next.js development server to call the API.
// Set CORS_ALLOWED_ORIGINS to a comma-separated list in production.
func cors(allowedOrigins []string) func(http.Handler) http.Handler {
	if len(allowedOrigins) == 0 {
		allowedOrigins = []string{"http://localhost:3000"}
	}
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			origin := r.Header.Get("Origin")
			for _, allowedOrigin := range allowedOrigins {
				if origin == strings.TrimSpace(allowedOrigin) {
					w.Header().Set("Access-Control-Allow-Origin", origin)
					w.Header().Set("Access-Control-Allow-Credentials", "true")
					w.Header().Set("Access-Control-Allow-Headers", "Content-Type")
					w.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS")
					w.Header().Add("Vary", "Origin")
					break
				}
			}

			if r.Method == http.MethodOptions {
				w.WriteHeader(http.StatusNoContent)
				return
			}
			next.ServeHTTP(w, r)
		})
	}
}
