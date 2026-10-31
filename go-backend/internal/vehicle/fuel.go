package vehicle

import (
	"context"
	"database/sql"
	"encoding/json"
	"errors"
	"github.com/jackc/pgx/v5/pgtype"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/go-chi/chi/v5"

	"github.com/DarkAbhi/life-backend/internal/vehicle/query"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type fuelItemInput struct {
	FuelType  string   `json:"fuel_type"`
	FillType  string   `json:"fill_type"`
	Quantity  *float64 `json:"quantity"`
	UnitPrice *float64 `json:"unit_price"`
	TotalCost *float64 `json:"total_cost"`
}

type fuelFillupInput struct {
	OdometerKM  float64         `json:"odometer_km"`
	FilledAt    *time.Time      `json:"filled_at"`
	StationName *string         `json:"station_name"`
	Notes       *string         `json:"notes"`
	Items       []fuelItemInput `json:"items"`
}

// CreateFuelFillup logs a new fuel fill-up event.
func (h *Handler) CreateFuelFillup(w http.ResponseWriter, r *http.Request) {
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
	var in fuelFillupInput
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<20)).Decode(&in); err != nil {
		webutil.BadRequest(w, "invalid json")
		return
	}
	if in.OdometerKM < 0 || len(in.Items) == 0 || len(in.Items) > 2 {
		webutil.BadRequest(w, "odometer and one or two fuel tanks are required")
		return
	}
	for i := range in.Items {
		if err := normalizeFuelItem(&in.Items[i]); err != nil {
			webutil.BadRequest(w, err.Error())
			return
		}
	}
	filledAt := time.Now().UTC()
	if in.FilledAt != nil {
		filledAt = *in.FilledAt
	}
	tx, err := h.DB.Begin(r.Context())
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	defer tx.Rollback(r.Context())
	q := query.New(tx)
	previousOdometer, err := q.GetMaxFuelOdometer(r.Context(), query.GetMaxFuelOdometerParams{VehicleID: vehicleID, UserID: user.ID})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	if in.OdometerKM < previousOdometer {
		webutil.BadRequest(w, "odometer cannot be lower than a previous fuel entry")
		return
	}
	fillupID, err := q.CreateFuelFillup(r.Context(), query.CreateFuelFillupParams{VehicleID: vehicleID, UserID: user.ID, OdometerKm: in.OdometerKM, FilledAt: pgtype.Timestamptz{Time: filledAt, Valid: true}, StationName: nullableString(in.StationName), Notes: nullableText(in.Notes)})
	if errors.Is(err, sql.ErrNoRows) {
		http.NotFound(w, r)
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	for _, item := range in.Items {
		if err := q.CreateFuelItem(r.Context(), query.CreateFuelItemParams{FillupID: fillupID, FuelType: item.FuelType, FillType: item.FillType, Quantity: *item.Quantity, UnitPrice: *item.UnitPrice, TotalCost: *item.TotalCost}); err != nil {
			webutil.ServerError(w, err)
			return
		}
	}
	if err := tx.Commit(r.Context()); err != nil {
		webutil.ServerError(w, err)
		return
	}
	economies := map[string]*float64{}
	for _, item := range in.Items {
		economies[item.FuelType] = h.latestFuelEconomy(vehicleID, user.ID, item.FuelType)
	}
	webutil.WriteJSON(w, http.StatusCreated, map[string]any{"id": fillupID, "economy_km_per_litre": economies})
}

// UpdateFuelFillup updates an existing fuel fillup record.
func (h *Handler) UpdateFuelFillup(w http.ResponseWriter, r *http.Request) {
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
	fillupID, err := strconv.ParseInt(chi.URLParam(r, "fillupID"), 10, 64)
	if err != nil || fillupID <= 0 {
		webutil.BadRequest(w, "invalid fuel fill-up id")
		return
	}
	var in fuelFillupInput
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<20)).Decode(&in); err != nil {
		webutil.BadRequest(w, "invalid json")
		return
	}
	if in.OdometerKM < 0 || len(in.Items) == 0 || len(in.Items) > 2 {
		webutil.BadRequest(w, "odometer and one or two fuel tanks are required")
		return
	}
	for i := range in.Items {
		if err := normalizeFuelItem(&in.Items[i]); err != nil {
			webutil.BadRequest(w, err.Error())
			return
		}
	}
	filledAt := time.Now().UTC()
	if in.FilledAt != nil {
		filledAt = *in.FilledAt
	}
	tx, err := h.DB.Begin(r.Context())
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	defer tx.Rollback(r.Context())
	q := query.New(tx)
	if _, err := q.LockFuelFillup(r.Context(), query.LockFuelFillupParams{ID: fillupID, VehicleID: vehicleID, UserID: user.ID}); errors.Is(err, sql.ErrNoRows) {
		http.NotFound(w, r)
		return
	} else if err != nil {
		webutil.ServerError(w, err)
		return
	}
	if err := q.UpdateFuelFillup(r.Context(), query.UpdateFuelFillupParams{OdometerKm: in.OdometerKM, FilledAt: pgtype.Timestamptz{Time: filledAt, Valid: true}, StationName: nullableString(in.StationName), Notes: nullableText(in.Notes), ID: fillupID}); err != nil {
		webutil.ServerError(w, err)
		return
	}
	if err := q.DeleteFuelItems(r.Context(), fillupID); err != nil {
		webutil.ServerError(w, err)
		return
	}
	for _, item := range in.Items {
		if err := q.CreateFuelItem(r.Context(), query.CreateFuelItemParams{FillupID: fillupID, FuelType: item.FuelType, FillType: item.FillType, Quantity: *item.Quantity, UnitPrice: *item.UnitPrice, TotalCost: *item.TotalCost}); err != nil {
			webutil.ServerError(w, err)
			return
		}
	}
	if err := tx.Commit(r.Context()); err != nil {
		webutil.ServerError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, map[string]any{"id": fillupID})
}

func normalizeFuelItem(item *fuelItemInput) error {
	item.FuelType = strings.ToLower(strings.TrimSpace(item.FuelType))
	item.FillType = strings.ToLower(strings.TrimSpace(item.FillType))
	if !map[string]bool{"petrol": true, "diesel": true, "lpg": true, "cng": true, "electric": true}[item.FuelType] {
		return errors.New("invalid fuel type")
	}
	if !map[string]bool{"full": true, "partial": true, "missed": true}[item.FillType] {
		return errors.New("invalid fill type")
	}
	count := 0
	if item.Quantity != nil {
		count++
	}
	if item.UnitPrice != nil {
		count++
	}
	if item.TotalCost != nil {
		count++
	}
	if count < 2 {
		return errors.New("enter any two of quantity, price, and total cost")
	}
	if item.Quantity == nil {
		v := *item.TotalCost / *item.UnitPrice
		item.Quantity = &v
	} else if item.UnitPrice == nil {
		v := *item.TotalCost / *item.Quantity
		item.UnitPrice = &v
	} else if item.TotalCost == nil {
		v := *item.Quantity * *item.UnitPrice
		item.TotalCost = &v
	}
	if *item.Quantity <= 0 || *item.UnitPrice <= 0 || *item.TotalCost <= 0 {
		return errors.New("fuel values must be positive")
	}
	return nil
}

func (h *Handler) latestFuelEconomy(vehicleID, userID int64, fuelType string) *float64 {
	rows, err := query.New(h.DB).ListFuelEconomyEntries(context.Background(), query.ListFuelEconomyEntriesParams{VehicleID: vehicleID, FuelType: fuelType, UserID: userID})
	if err != nil {
		return nil
	}
	entries := make([]fuelMileageEntry, 0, len(rows))
	for _, row := range rows {
		entries = append(entries, fuelMileageEntry{
			FuelType: fuelType, FillType: row.FillType,
			OdometerKM: row.OdometerKm, Quantity: row.Quantity,
		})
	}
	return calculateFuelEfficiency(entries)[fuelType].LastKMPerLitre
}

type fuelMileageEntry struct {
	FuelType   string
	FillType   string
	OdometerKM float64
	Quantity   float64
	TotalCost  float64
}

type fuelEfficiencyStats struct {
	TotalCost         float64  `json:"total_cost"`
	TotalVolume       float64  `json:"total_volume"`
	AverageKMPerLitre *float64 `json:"average_km_per_litre"`
	MaxKMPerLitre     *float64 `json:"max_km_per_litre"`
	MinKMPerLitre     *float64 `json:"min_km_per_litre"`
	LastKMPerLitre    *float64 `json:"last_km_per_litre"`
}

// calculateFuelEfficiency uses chronological entries and complete full-to-full
// intervals. Partial fills contribute to the next interval; missed fills reset
// the baseline. Spending and volume include every recorded fill.
func calculateFuelEfficiency(entries []fuelMileageEntry) map[string]fuelEfficiencyStats {
	type intervalState struct {
		previousFull *float64
		accumulated  float64
		distance     float64
		quantity     float64
	}
	states := map[string]intervalState{}
	stats := map[string]fuelEfficiencyStats{}
	for _, entry := range entries {
		stat := stats[entry.FuelType]
		stat.TotalCost += entry.TotalCost
		stat.TotalVolume += entry.Quantity
		state := states[entry.FuelType]
		switch entry.FillType {
		case "missed":
			state.previousFull = nil
			state.accumulated = 0
		case "partial":
			if state.previousFull != nil {
				state.accumulated += entry.Quantity
			}
		case "full":
			quantity := state.accumulated + entry.Quantity
			if state.previousFull != nil && entry.OdometerKM > *state.previousFull && quantity > 0 {
				distance := entry.OdometerKM - *state.previousFull
				economy := distance / quantity
				state.distance += distance
				state.quantity += quantity
				average := state.distance / state.quantity
				stat.AverageKMPerLitre = &average
				stat.LastKMPerLitre = &economy
				if stat.MaxKMPerLitre == nil || economy > *stat.MaxKMPerLitre {
					stat.MaxKMPerLitre = &economy
				}
				if stat.MinKMPerLitre == nil || economy < *stat.MinKMPerLitre {
					stat.MinKMPerLitre = &economy
				}
			}
			odometer := entry.OdometerKM
			state.previousFull = &odometer
			state.accumulated = 0
		}
		states[entry.FuelType] = state
		stats[entry.FuelType] = stat
	}
	return stats
}
