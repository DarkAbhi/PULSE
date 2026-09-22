package main

import (
	"flag"
	"log"
	"net/http"
	"os"
	"strconv"
	"time"

	"github.com/go-chi/chi/v5/middleware"
	"github.com/joho/godotenv"

	db "github.com/DarkAbhi/life-backend/internal/db"
	"github.com/DarkAbhi/life-backend/internal/gym"
	"github.com/DarkAbhi/life-backend/internal/handlers"
	"github.com/DarkAbhi/life-backend/internal/mealplan"
	"github.com/DarkAbhi/life-backend/internal/vehicle"
)

func main() {
	// Keep operational logs in UTC so they are comparable across environments.
	log.SetFlags(log.LstdFlags | log.LUTC | log.Lmicroseconds)

	// Loads .env if present; harmless in containers if absent
	_ = godotenv.Load()

	// CLI flags used by the one-shot "migrate" container
	migrateUp := flag.Bool("migrate-up", false, "Run all pending migrations")
	rollback := flag.Bool("rollback", false, "Rollback last migration")
	showVersion := flag.Bool("version", false, "Show current migration version")
	steps := flag.String("steps", "", "Run N migration steps (negative to rollback)")
	flag.Parse()

	switch {
	case *steps != "":
		n, err := strconv.Atoi(*steps)
		if err != nil {
			log.Fatalf("❌ invalid --steps: %v", err)
		}
		if err := db.RunMigrationSteps(n); err != nil {
			log.Fatalf("❌ steps failed: %v", err)
		}
		log.Printf("✅ steps=%d applied successfully\n", n)
		os.Exit(0)

	case *migrateUp:
		if err := db.RunMigrations(); err != nil {
			log.Fatalf("❌ migrate up failed: %v", err)
		}
		log.Println("✅ migrate up: success")
		os.Exit(0)

	case *rollback:
		if err := db.RollbackMigration(); err != nil {
			log.Fatalf("❌ rollback failed: %v", err)
		}
		log.Println("✅ rollback: success")
		os.Exit(0)

	case *showVersion:
		if err := db.ShowMigrationVersion(); err != nil {
			log.Fatalf("❌ version failed: %v", err)
		}
		// ShowVersion prints and returns nil when OK
		os.Exit(0)
	}

	// --- Normal server boot ---
	d := db.ConnectDB()
	if err := mealplan.EnsureDefaultMealTimes(d); err != nil {
		log.Printf("⚠️ warning: failed to ensure default meal times: %v\n", err)
	}
	go vehicle.RunAirFillReminderJob(d)
	go gym.RunGymReminderJob(d)

	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	api := &handlers.API{DB: d}
	r := api.Router()

	// If your Router() returns a chi.Router, prefer r.Use(...).
	// What you have works if Router() returns an http.Handler; keeping your pattern:
	r = middleware.Logger(r)
	r = middleware.Recoverer(r)

	srv := &http.Server{
		Addr:              ":" + port,
		Handler:           r,
		ReadHeaderTimeout: 5 * time.Second,
		IdleTimeout:       60 * time.Second,
	}

	log.Printf("🚀 Server starting on :%s\n", port)
	log.Fatal(srv.ListenAndServe())
}
