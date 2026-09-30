package vehicle

import (
	"database/sql"
	"errors"
	"net/http"
	"time"

	"github.com/DarkAbhi/life-backend/internal/vehicle/query"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type airFillDTO struct {
	VehicleID int64     `json:"vehicle_id"`
	FilledAt  time.Time `json:"filled_at"`
}

// CreateAirFill records a new air fill event for a vehicle.
func (h *Handler) CreateAirFill(w http.ResponseWriter, r *http.Request) {
	user, err := h.sessionUser(r)
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

	q := query.New(h.DB)
	hasVehicle, err := q.VehicleExists(r.Context(), query.VehicleExistsParams{ID: vehicleID, UserID: user.ID})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	if !hasVehicle {
		http.NotFound(w, r)
		return
	}

	filledAt, err := q.CreateVehicleAirFill(r.Context(), query.CreateVehicleAirFillParams{VehicleID: vehicleID, UserID: user.ID})
	if errors.Is(err, sql.ErrNoRows) {
		http.NotFound(w, r)
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusCreated, airFillDTO{VehicleID: vehicleID, FilledAt: filledAt.Time.UTC()})
}

// ListAirFills lists a vehicle's air fills, newest first.
func (h *Handler) ListAirFills(w http.ResponseWriter, r *http.Request) {
	user, err := h.sessionUser(r)
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
	rows, err := query.New(h.DB).ListVehicleAirFills(r.Context(), query.ListVehicleAirFillsParams{VehicleID: vehicleID, UserID: user.ID})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	fills := make([]airFillHistory, 0, len(rows))
	for _, row := range rows {
		fills = append(fills, airFillHistory{ID: row.ID, FilledAt: row.FilledAt.Time.UTC()})
	}
	webutil.WriteJSON(w, http.StatusOK, fills)
}
