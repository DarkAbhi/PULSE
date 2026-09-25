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
	hasVehicle, err := q.VehicleExists(r.Context(), vehicleID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	if !hasVehicle {
		http.NotFound(w, r)
		return
	}

	filledAt, err := q.CreateVehicleAirFill(r.Context(), query.CreateVehicleAirFillParams{VehicleID: vehicleID, UserID: user.ID})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusCreated, airFillDTO{VehicleID: vehicleID, FilledAt: filledAt.Time.UTC()})
}

// ListLatestAirFills lists the latest air fills across vehicles.
func (h *Handler) ListLatestAirFills(w http.ResponseWriter, r *http.Request) {
	user, err := h.sessionUser(r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	rows, err := query.New(h.DB).ListLatestVehicleAirFills(r.Context(), user.ID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	fills := make([]airFillDTO, 0)
	for _, row := range rows {
		fill := airFillDTO{VehicleID: row.VehicleID, FilledAt: row.FilledAt.Time}
		fill.FilledAt = fill.FilledAt.UTC()
		fills = append(fills, fill)
	}
	webutil.WriteJSON(w, http.StatusOK, fills)
}
