package vehicle

import (
	"context"
	"database/sql"
	"errors"
	"log"
	"net/http"
	"time"

	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/DarkAbhi/life-backend/internal/db/sqlc"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type vehicleAirFillDTO struct {
	VehicleID int64     `json:"vehicle_id"`
	FilledAt  time.Time `json:"filled_at"`
}

// CreateVehicleAirFill records a new air fill event for a vehicle.
func (h *Handler) CreateVehicleAirFill(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	vehicleID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}

	q := sqlc.New(h.DB)
	vehicleExists, err := q.VehicleExists(r.Context(), vehicleID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	if !vehicleExists {
		http.NotFound(w, r)
		return
	}

	filledAt, err := q.CreateVehicleAirFill(r.Context(), sqlc.CreateVehicleAirFillParams{VehicleID: vehicleID, UserID: user.ID})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusCreated, vehicleAirFillDTO{VehicleID: vehicleID, FilledAt: filledAt.UTC()})
}

// ListLatestVehicleAirFills lists the latest air fills across vehicles.
func (h *Handler) ListLatestVehicleAirFills(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	rows, err := sqlc.New(h.DB).ListLatestVehicleAirFills(r.Context(), user.ID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	fills := make([]vehicleAirFillDTO, 0)
	for _, row := range rows {
		fill := vehicleAirFillDTO{VehicleID: row.VehicleID, FilledAt: row.FilledAt}
		fill.FilledAt = fill.FilledAt.UTC()
		fills = append(fills, fill)
	}
	webutil.WriteJSON(w, http.StatusOK, fills)
}

// RunAirFillReminderJob creates each overdue air-fill reminder once.
// It catches up on startup and then checks hourly while the API is running.
func RunAirFillReminderJob(database *sql.DB) {
	createDueAirFillReminders(database)
	ticker := time.NewTicker(time.Hour)
	defer ticker.Stop()
	for range ticker.C {
		createDueAirFillReminders(database)
	}
}

func createDueAirFillReminders(database *sql.DB) {
	rows, err := sqlc.New(database).ListDueAirFillReminders(context.Background())
	if err != nil {
		log.Printf("air-fill reminder scan failed: %v", err)
		return
	}
	for _, airFillID := range rows {
		if err := createAirFillReminder(database, airFillID); err != nil {
			log.Printf("air-fill reminder failed for fill %d: %v", airFillID, err)
		}
	}
}

func createAirFillReminder(database *sql.DB, airFillID int64) error {
	tx, err := database.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()
	q := sqlc.New(tx)

	row, err := q.LockDueAirFillReminder(context.Background(), airFillID)
	if errors.Is(err, sql.ErrNoRows) {
		return nil
	}
	if err != nil {
		return err
	}

	notificationID, err := q.CreateAirFillNotification(context.Background(), sqlc.CreateAirFillNotificationParams{UserID: row.UserID, Title: "Time to check " + row.Name + "'s air", Body: sql.NullString{String: "It has been 30 days since you last filled air in " + row.Name + ".", Valid: true}, Column4: row.VehicleID})
	if err != nil {
		return err
	}
	if err := q.LinkAirFillReminder(context.Background(), sqlc.LinkAirFillReminderParams{ReminderNotificationID: sql.NullInt64{Int64: notificationID, Valid: true}, ID: airFillID}); err != nil {
		return err
	}
	return tx.Commit()
}
