package handlers

import (
	"database/sql"
	"net/http"
	"os"
	"strings"

	"github.com/go-chi/chi/v5"

	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/DarkAbhi/life-backend/internal/gym"
	"github.com/DarkAbhi/life-backend/internal/health"
	"github.com/DarkAbhi/life-backend/internal/horizon"
	"github.com/DarkAbhi/life-backend/internal/meditation"
	"github.com/DarkAbhi/life-backend/internal/notification"
	"github.com/DarkAbhi/life-backend/internal/profile"
	"github.com/DarkAbhi/life-backend/internal/purchase"
	"github.com/DarkAbhi/life-backend/internal/sport"
	"github.com/DarkAbhi/life-backend/internal/vehicle"
)

type API struct {
	DB *sql.DB
}

func (a *API) Router() http.Handler {
	if a.DB == nil {
		panic("handlers.API DB is nil — did you forget to ConnectDB and pass it in?")
	}
	r := chi.NewRouter()
	r.Use(cors)

	authHandler := auth.NewHandler(a.DB)
	healthHandler := health.NewHandler(a.DB)
	profileHandler := profile.NewHandler(a.DB)
	notifHandler := notification.NewHandler(a.DB)
	purchaseHandler := purchase.NewHandler(a.DB)
	gymHandler := gym.NewHandler(a.DB)
	meditationHandler := meditation.NewHandler(a.DB)
	sportHandler := sport.NewHandler(a.DB)
	vehicleHandler := vehicle.NewHandler(a.DB)
	horizonHandler := horizon.NewHandler(a.DB)

	// Health (outside /api so Docker or Kubernetes health probes stay simple)
	r.Get("/healthz", healthHandler.Healthz) // liveness
	r.Get("/readyz", healthHandler.Readyz)   // readiness (DB ping)

	// All application APIs under /api
	r.Route("/api", func(api chi.Router) {
		api.Post("/auth/login", authHandler.Login)
		api.Get("/auth/session", authHandler.Session)
		api.Post("/auth/logout", authHandler.Logout)
		api.Get("/profile", profileHandler.GetProfile)
		api.Put("/profile", profileHandler.SaveProfile)
		api.Put("/profile/password", profileHandler.ChangePassword)
		api.Get("/notifications", notifHandler.ListNotifications)
		api.Delete("/notifications", notifHandler.ClearNotifications)
		api.Delete("/notifications/{id}", notifHandler.DismissNotification)
		api.Post("/notifications/{id}/gym-visit", gymHandler.MarkGymReminderVisited)
		api.Get("/next-month-purchases", purchaseHandler.NextMonthPurchases)
		api.Post("/next-month-purchases", purchaseHandler.CreateNextMonthPurchase)
		api.Delete("/next-month-purchases", purchaseHandler.ClearNextMonthPurchases)
		api.Delete("/next-month-purchases/{id}", purchaseHandler.DeleteNextMonthPurchase)

		// Financial Horizon
		api.Get("/horizon", horizonHandler.GetHorizon)
		api.Put("/horizon/config", horizonHandler.UpdateConfig)
		api.Post("/horizon/budgets", horizonHandler.CreateBudget)
		api.Put("/horizon/budgets/{id}", horizonHandler.UpdateBudget)
		api.Delete("/horizon/budgets/{id}", horizonHandler.DeleteBudget)
		api.Post("/horizon/deductions", horizonHandler.CreateDeduction)
		api.Put("/horizon/deductions/{id}", horizonHandler.UpdateDeduction)
		api.Delete("/horizon/deductions/{id}", horizonHandler.DeleteDeduction)
		api.Get("/horizon/categories", horizonHandler.ListCategories)
		api.Post("/horizon/categories", horizonHandler.CreateCategory)
		api.Get("/horizon/transactions", horizonHandler.ListTransactions)
		api.Post("/horizon/transactions", horizonHandler.CreateTransaction)
		api.Post("/horizon/transactions/bulk", horizonHandler.BulkCreateTransactions)
		api.Put("/horizon/transactions/{id}", horizonHandler.UpdateTransaction)
		api.Delete("/horizon/transactions/{id}", horizonHandler.DeleteTransaction)
		api.Get("/horizon/subscriptions", horizonHandler.ListSubscriptions)
		api.Post("/horizon/subscriptions", horizonHandler.CreateSubscription)
		api.Put("/horizon/subscriptions/{id}", horizonHandler.UpdateSubscription)
		api.Delete("/horizon/subscriptions/{id}", horizonHandler.DeleteSubscription)
		api.Get("/horizon/subscriptions/{id}/transactions", horizonHandler.ListSubscriptionTransactions)
		// Daily logs
		api.Get("/workout/today", gymHandler.GymVisitedToday)
		api.Post("/workout/today", gymHandler.AddWorkoutForDay)
		api.Get("/gym-visits", gymHandler.ListGymVisits)
		api.Delete("/gym-visits/{id}", gymHandler.DeleteGymVisit)
		api.Get("/gym-visits/{id}/exercises", gymHandler.GetGymVisitExercises)
		api.Post("/gym-visits/{id}/exercises", gymHandler.CreateGymVisitExercise)
		api.Post("/meditation/today", meditationHandler.AddMeditationForDay)
		api.Post("/sport/today", sportHandler.AddSportForDay)

		// Vehicles
		api.Get("/vehicles", vehicleHandler.ListVehicles)
		api.Post("/vehicles", vehicleHandler.CreateVehicle) // create
		api.Get("/vehicle-air-fills/latest", vehicleHandler.ListLatestVehicleAirFills)

		api.Route("/vehicles/{id}", func(v chi.Router) {
			v.Get("/", vehicleHandler.GetVehicle)       // retrieve by id
			v.Put("/", vehicleHandler.UpdateVehicle)    // full/partial update
			v.Patch("/", vehicleHandler.UpdateVehicle)  // alias to update
			v.Delete("/", vehicleHandler.DeleteVehicle) // delete
			v.Post("/air-fills", vehicleHandler.CreateVehicleAirFill)
			v.Post("/fuel-fillups", vehicleHandler.CreateFuelFillup)
			v.Put("/fuel-fillups/{fillupID}", vehicleHandler.UpdateFuelFillup)
			v.Get("/history", vehicleHandler.VehicleHistory)
			v.Delete("/air-fills/{airFillID}", vehicleHandler.DeleteVehicleAirFill)
			v.Delete("/fuel-fillups/{fillupID}", vehicleHandler.DeleteFuelFillup)
		})
	})

	return r
}

// cors permits the separately hosted Next.js development server to call the API.
// Set CORS_ALLOWED_ORIGINS to a comma-separated list in production.
func cors(next http.Handler) http.Handler {
	allowedOrigins := strings.Split(os.Getenv("CORS_ALLOWED_ORIGINS"), ",")
	if os.Getenv("CORS_ALLOWED_ORIGINS") == "" {
		allowedOrigins = []string{"http://localhost:3000"}
	}

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
