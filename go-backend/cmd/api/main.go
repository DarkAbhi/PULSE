package main

import (
	"context"
	"database/sql"
	"errors"
	"flag"
	"fmt"
	"log/slog"
	"net"
	"net/http"
	"net/url"
	"os"
	"os/signal"
	"strconv"
	"strings"
	"syscall"
	"time"

	_ "github.com/DarkAbhi/life-backend/docs"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/joho/godotenv"

	"github.com/DarkAbhi/life-backend/internal/activity"
	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/DarkAbhi/life-backend/internal/db"
	"github.com/DarkAbhi/life-backend/internal/gym"
	"github.com/DarkAbhi/life-backend/internal/horizon"
	"github.com/DarkAbhi/life-backend/internal/mealplan"
	"github.com/DarkAbhi/life-backend/internal/notification"
	"github.com/DarkAbhi/life-backend/internal/observability"
	"github.com/DarkAbhi/life-backend/internal/profile"
	"github.com/DarkAbhi/life-backend/internal/purchase"
	"github.com/DarkAbhi/life-backend/internal/vehicle"
)

// @title Life Backend API
// @version 1.0
// @description REST API for the Life Backend application.
// @host localhost:8080
// @BasePath /
// @schemes http https

func main() {
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	if err := run(ctx); err != nil {
		slog.Error("api stopped", "error", err)
		os.Exit(1)
	}
}

func run(ctx context.Context) error {
	_ = godotenv.Load()
	migrateUp := flag.Bool("migrate-up", false, "Run all pending migrations")
	rollback := flag.Bool("rollback", false, "Rollback last migration")
	showVersion := flag.Bool("version", false, "Show current migration version")
	steps := flag.String("steps", "", "Run N migration steps (negative to rollback)")
	flag.Parse()
	dsn := databaseDSN()
	switch {
	case *steps != "":
		n, err := strconv.Atoi(*steps)
		if err != nil {
			return fmt.Errorf("invalid --steps: %w", err)
		}
		return db.RunMigrationSteps(dsn, n)
	case *migrateUp:
		return db.RunMigrations(dsn)
	case *rollback:
		return db.RollbackMigration(dsn)
	case *showVersion:
		return db.ShowMigrationVersion(dsn)
	}

	obs, err := observability.Setup(ctx)
	if err != nil {
		return fmt.Errorf("setup observability: %w", err)
	}
	defer func() {
		shutdownCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		_ = obs.Shutdown(shutdownCtx)
	}()

	pool, err := pgxpool.New(ctx, dsn)
	if err != nil {
		return fmt.Errorf("open postgres pool: %w", err)
	}
	defer pool.Close()
	observability.RegisterDBStatsCollector(pool)
	pingCtx, cancel := context.WithTimeout(ctx, 5*time.Second)
	err = pool.Ping(pingCtx)
	cancel()
	if err != nil {
		return fmt.Errorf("ping postgres: %w", err)
	}

	authRepository := auth.NewRepository(pool)
	authService := auth.NewService(authRepository)
	sessionLookup := func(r *http.Request) (auth.SessionUser, error) {
		user, err := authService.SessionUser(r.Context(), auth.ExtractSessionToken(r))
		if errors.Is(err, auth.ErrSessionNotFound) {
			return auth.SessionUser{}, sql.ErrNoRows
		}
		return user, err
	}
	sessionID := func(r *http.Request) (int64, error) {
		user, err := sessionLookup(r)
		return user.ID, err
	}
	activityRepository := activity.NewRepository(pool)
	activityService := activity.NewService(activityRepository)
	purchaseRepository := purchase.NewRepository(pool)
	purchaseService := purchase.NewService(purchaseRepository)
	mealRepository := mealplan.NewRepository(pool)
	mealService := mealplan.NewService(mealRepository)
	notificationRepository := notification.NewRepository(pool)
	notificationService := notification.NewService(notificationRepository)
	gymRepository := gym.NewRepository(pool)
	gymService := gym.NewService(gymRepository, notificationService, authService)
	profileRepository := profile.NewRepository(pool)
	profileService := profile.NewService(profileRepository, authService)
	vehicleRepository := vehicle.NewRepository(pool)
	vehicleService := vehicle.NewService(vehicleRepository)
	horizonService := horizon.NewService(pool)
	if err := mealService.EnsureDefaults(ctx); err != nil {
		slog.Warn("ensure default meal times failed", "error", err)
	}
	var allowedOrigins []string
	if raw := os.Getenv("CORS_ALLOWED_ORIGINS"); raw != "" {
		allowedOrigins = strings.Split(raw, ",")
	}
	vehicleHandler := vehicle.NewHandler(pool, vehicleService, sessionLookup)
	if err := vehicleHandler.ConfigureAttachments(ctx, vehicle.AttachmentConfig{
		Bucket: os.Getenv("S3_BUCKET"), Region: os.Getenv("AWS_REGION"),
		Endpoint: os.Getenv("S3_ENDPOINT"), ForcePathStyle: os.Getenv("S3_FORCE_PATH_STYLE") == "true",
	}); err != nil {
		return fmt.Errorf("configure vehicle attachments: %w", err)
	}

	api := &API{
		DB:             pool,
		AllowedOrigins: allowedOrigins,
		Activity:       activity.NewHandler(activityService),
		Purchase:       purchase.NewHandler(purchaseService, sessionID),
		MealPlan:       mealplan.NewHandler(mealService, sessionID),
		Notification:   notification.NewHandler(notificationService, sessionID),
		Gym:            gym.NewHandler(gymService, sessionID),
		Auth:           auth.NewHandler(authService, os.Getenv("APP_ENV") == "production"),
		Vehicle:        vehicleHandler,
		Horizon:        horizon.NewHandler(pool, horizonService, sessionLookup),
		Profile:        profile.NewHandler(profileService, sessionID),
		ObsConfig:      obs.Config,
	}
	handler := api.Router()
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}
	server := &http.Server{Addr: ":" + port, Handler: handler, ReadHeaderTimeout: 5 * time.Second, IdleTimeout: 60 * time.Second}
	go gym.RunGymReminderJob(ctx, gymService)
	go vehicle.RunAirFillReminderJob(ctx, vehicle.NewReminderService(pool, notificationService))
	serveErr := make(chan error, 1)
	go func() { serveErr <- server.ListenAndServe() }()
	slog.Info("api listening", "port", port)
	select {
	case err := <-serveErr:
		if errors.Is(err, http.ErrServerClosed) {
			return nil
		}
		return err
	case <-ctx.Done():
		shutdownCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer cancel()
		if err := server.Shutdown(shutdownCtx); err != nil {
			return fmt.Errorf("shutdown server: %w", err)
		}
		return nil
	}
}

func databaseDSN() string {
	get := func(key, fallback string) string {
		if value := os.Getenv(key); value != "" {
			return value
		}
		return fallback
	}
	u := url.URL{Scheme: "postgres", User: url.UserPassword(get("DB_USERNAME", "user"), get("DB_PASSWORD", "pass")), Host: net.JoinHostPort(get("DB_HOSTNAME", "localhost"), get("DB_PORT", "5432")), Path: "/" + get("DB_NAME", "life")}
	q := u.Query()
	q.Set("sslmode", get("DB_SSLMODE", "disable"))
	q.Set("connect_timeout", "5")
	q.Set("timezone", "UTC")
	u.RawQuery = q.Encode()
	return u.String()
}
