package vehicle

import (
	"database/sql"
	"encoding/json"
	"errors"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/go-chi/chi/v5"

	"github.com/DarkAbhi/life-backend/internal/auth"
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
	tx, err := h.DB.BeginTx(r.Context(), nil)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	defer tx.Rollback()
	var previousOdometer sql.NullFloat64
	if err := tx.QueryRow(`SELECT MAX(odometer_km) FROM vehicle_fuel_fillups WHERE vehicle_id=$1`, vehicleID).Scan(&previousOdometer); err != nil {
		webutil.ServerError(w, err)
		return
	}
	if previousOdometer.Valid && in.OdometerKM < previousOdometer.Float64 {
		webutil.BadRequest(w, "odometer cannot be lower than a previous fuel entry")
		return
	}
	var fillupID int64
	if err := tx.QueryRow(`INSERT INTO vehicle_fuel_fillups (vehicle_id,user_id,odometer_km,filled_at,station_name,notes) VALUES ($1,$2,$3,$4,$5,$6) RETURNING id`, vehicleID, user.ID, in.OdometerKM, filledAt, in.StationName, in.Notes).Scan(&fillupID); err != nil {
		webutil.ServerError(w, err)
		return
	}
	for _, item := range in.Items {
		if _, err := tx.Exec(`INSERT INTO vehicle_fuel_items (fillup_id,fuel_type,fill_type,quantity,unit_price,total_cost) VALUES ($1,$2,$3,$4,$5,$6)`, fillupID, item.FuelType, item.FillType, *item.Quantity, *item.UnitPrice, *item.TotalCost); err != nil {
			webutil.ServerError(w, err)
			return
		}
	}
	if err := tx.Commit(); err != nil {
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
	tx, err := h.DB.BeginTx(r.Context(), nil)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	defer tx.Rollback()
	var found int
	if err := tx.QueryRow(`SELECT 1 FROM vehicle_fuel_fillups WHERE id=$1 AND vehicle_id=$2 AND user_id=$3 FOR UPDATE`, fillupID, vehicleID, user.ID).Scan(&found); errors.Is(err, sql.ErrNoRows) {
		http.NotFound(w, r)
		return
	} else if err != nil {
		webutil.ServerError(w, err)
		return
	}
	if _, err := tx.Exec(`UPDATE vehicle_fuel_fillups SET odometer_km=$1,filled_at=$2,station_name=$3,notes=$4 WHERE id=$5`, in.OdometerKM, filledAt, in.StationName, in.Notes, fillupID); err != nil {
		webutil.ServerError(w, err)
		return
	}
	if _, err := tx.Exec(`DELETE FROM vehicle_fuel_items WHERE fillup_id=$1`, fillupID); err != nil {
		webutil.ServerError(w, err)
		return
	}
	for _, item := range in.Items {
		if _, err := tx.Exec(`INSERT INTO vehicle_fuel_items (fillup_id,fuel_type,fill_type,quantity,unit_price,total_cost) VALUES ($1,$2,$3,$4,$5,$6)`, fillupID, item.FuelType, item.FillType, *item.Quantity, *item.UnitPrice, *item.TotalCost); err != nil {
			webutil.ServerError(w, err)
			return
		}
	}
	if err := tx.Commit(); err != nil {
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
	rows, err := h.DB.Query(`SELECT f.odometer_km, i.fill_type, i.quantity FROM vehicle_fuel_fillups f JOIN vehicle_fuel_items i ON i.fillup_id=f.id WHERE f.vehicle_id=$1 AND i.fuel_type=$2 ORDER BY f.filled_at, f.id`, vehicleID, fuelType)
	if err != nil {
		return nil
	}
	defer rows.Close()
	var previousFull *float64
	accumulated := 0.0
	var latest *float64
	for rows.Next() {
		var odometer, quantity float64
		var fillType string
		if rows.Scan(&odometer, &fillType, &quantity) != nil {
			return nil
		}
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
	rows, err := h.DB.Query(`SELECT i.fuel_type, f.odometer_km, i.fill_type, i.quantity FROM vehicle_fuel_fillups f JOIN vehicle_fuel_items i ON i.fillup_id=f.id WHERE f.vehicle_id=$1 AND f.user_id=$2 ORDER BY i.fuel_type, f.filled_at, f.id`, vehicleID, userID)
	if err != nil {
		return map[string]float64{}
	}
	defer rows.Close()

	entries := []fuelMileageEntry{}
	for rows.Next() {
		var entry fuelMileageEntry
		if err := rows.Scan(&entry.FuelType, &entry.OdometerKM, &entry.FillType, &entry.Quantity); err != nil {
			return map[string]float64{}
		}
		entries = append(entries, entry)
	}
	if rows.Err() != nil {
		return map[string]float64{}
	}
	return calculateAverageFuelEconomies(entries)
}
