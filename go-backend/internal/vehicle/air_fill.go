package vehicle

import (
	"database/sql"
	"encoding/json"
	"errors"
	"io"
	"math"
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
	var input struct {
		OdometerKM *float64 `json:"odometer_km"`
	}
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1024)).Decode(&input); err != nil && !errors.Is(err, io.EOF) {
		webutil.BadRequest(w, "invalid json")
		return
	}
	if input.OdometerKM != nil && (math.IsNaN(*input.OdometerKM) || math.IsInf(*input.OdometerKM, 0) || *input.OdometerKM < 0 || *input.OdometerKM >= 1e11) {
		webutil.BadRequest(w, "invalid odometer")
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

	filledAt, err := q.CreateVehicleAirFill(r.Context(), query.CreateVehicleAirFillParams{VehicleID: vehicleID, UserID: user.ID, OdometerKm: input.OdometerKM})
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
