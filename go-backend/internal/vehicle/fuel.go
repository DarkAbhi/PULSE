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
		webutil.BadRequest(w, "invalid JSON")
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
	previousOdometer, err := q.GetMaxFuelOdometer(r.Context(), vehicleID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	if in.OdometerKM < previousOdometer {
		webutil.BadRequest(w, "odometer cannot be lower than a previous fuel entry")
		return
	}
	fillupID, err := q.CreateFuelFillup(r.Context(), query.CreateFuelFillupParams{VehicleID: vehicleID, UserID: user.ID, OdometerKm: in.OdometerKM, FilledAt: pgtype.Timestamptz{Time: filledAt, Valid: true}, StationName: nullableString(in.StationName), Notes: nullableText(in.Notes)})
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
		economies[item.FuelType] = h.latestFuelEconomy(vehicleID, item.FuelType)
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
		webutil.BadRequest(w, "invalid JSON")
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

func (h *Handler) latestFuelEconomy(vehicleID int64, fuelType string) *float64 {
	rows, err := query.New(h.DB).ListFuelEconomyEntries(context.Background(), query.ListFuelEconomyEntriesParams{VehicleID: vehicleID, FuelType: fuelType})
	if err != nil {
		return nil
	}
	var previousFull *float64
	accumulated := 0.0
	var latest *float64
	for _, row := range rows {
		odometer, quantity, fillType := row.OdometerKm, row.Quantity, row.FillType
		if fillType == "missed" {
			previousFull = nil
			accumulated = 0
			continue
		}
		if fillType == "partial" {
			if previousFull != nil {
				accumulated += quantity
			}
			continue
		}
		if previousFull != nil && odometer > *previousFull {
			value := (odometer - *previousFull) / (accumulated + quantity)
			latest = &value
		}
		value := odometer
		previousFull = &value
		accumulated = 0
	}
	return latest
}

type fuelMileageEntry struct {
	FuelType   string
	FillType   string
	OdometerKM float64
	Quantity   float64
}

// calculateAverageFuelEconomies returns the weighted average fuel economy for
// each fuel type. Entries must be supplied in chronological order. It only uses
// complete full-tank-to-full-tank intervals; partial fills between those two
// readings are included, while missed fills reset the calculation because their
// fuel use is unknown.
func calculateAverageFuelEconomies(entries []fuelMileageEntry) map[string]float64 {
	type totals struct {
		distance float64
		quantity float64
	}

	previousFull := map[string]*float64{}
	accumulated := map[string]float64{}
	totalsByFuel := map[string]totals{}
	for _, entry := range entries {
		if entry.FillType == "missed" {
			previousFull[entry.FuelType] = nil
			accumulated[entry.FuelType] = 0
			continue
		}
		if entry.FillType == "partial" {
			if previousFull[entry.FuelType] != nil {
				accumulated[entry.FuelType] += entry.Quantity
			}
			continue
		}

		if previous := previousFull[entry.FuelType]; previous != nil && entry.OdometerKM > *previous {
			total := totalsByFuel[entry.FuelType]
			total.distance += entry.OdometerKM - *previous
			total.quantity += accumulated[entry.FuelType] + entry.Quantity
			totalsByFuel[entry.FuelType] = total
		}
		value := entry.OdometerKM
		previousFull[entry.FuelType] = &value
		accumulated[entry.FuelType] = 0
	}

	averages := map[string]float64{}
	for fuelType, total := range totalsByFuel {
		if total.quantity > 0 {
			averages[fuelType] = total.distance / total.quantity
		}
	}
	return averages
}

func (h *Handler) averageFuelEconomies(vehicleID, userID int64) map[string]float64 {
	rows, err := query.New(h.DB).ListAverageFuelEconomyEntries(context.Background(), query.ListAverageFuelEconomyEntriesParams{VehicleID: vehicleID, UserID: userID})
	if err != nil {
		return map[string]float64{}
	}

	entries := []fuelMileageEntry{}
	for _, row := range rows {
		entry := fuelMileageEntry{FuelType: row.FuelType, OdometerKM: row.OdometerKm, FillType: row.FillType, Quantity: row.Quantity}
		entries = append(entries, entry)
	}
	return calculateAverageFuelEconomies(entries)
}
