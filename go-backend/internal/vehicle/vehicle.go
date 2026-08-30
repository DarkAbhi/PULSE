package vehicle

import (
	"database/sql"
	"encoding/json"
	"errors"
	"net/http"
	"strconv"
	"time"

	"github.com/go-chi/chi/v5"

	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type vehiclePayload struct {
	Name     *string `json:"name"`
	IsActive *bool   `json:"is_active"`
}

type vehicleDTO struct {
	ID       int64  `json:"id"`
	Name     string `json:"name"`
	IsActive bool   `json:"is_active"`
}

type airFillHistory struct {
	ID       int64     `json:"id"`
	FilledAt time.Time `json:"filled_at"`
}

type fuelHistoryItem struct {
	FuelType  string  `json:"fuel_type"`
	FillType  string  `json:"fill_type"`
	Quantity  float64 `json:"quantity"`
	UnitPrice float64 `json:"unit_price"`
	TotalCost float64 `json:"total_cost"`
}

type fuelFillHistory struct {
	ID          int64             `json:"id"`
	OdometerKM  float64           `json:"odometer_km"`
	FilledAt    time.Time         `json:"filled_at"`
	StationName *string           `json:"station_name"`
	Notes       *string           `json:"notes"`
	Items       []fuelHistoryItem `json:"items"`
}

type Handler struct {
	DB *sql.DB
}

func NewHandler(db *sql.DB) *Handler {
	return &Handler{DB: db}
}

// ListVehicles lists all vehicles.
func (h *Handler) ListVehicles(w http.ResponseWriter, r *http.Request) {
	const q = `SELECT id, name FROM vehicles ORDER BY id ASC;`
	rows, err := h.DB.Query(q)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	defer rows.Close()

	type vehicleItem struct {
		ID   int64  `json:"id"`
		Name string `json:"name"`
	}
	out := make([]vehicleItem, 0)
	for rows.Next() {
		var v vehicleItem
		if err := rows.Scan(&v.ID, &v.Name); err != nil {
			webutil.ServerError(w, err)
			return
		}
		out = append(out, v)
	}
	if err := rows.Err(); err != nil {
		webutil.ServerError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, out)
}

// CreateVehicle creates a new vehicle.
func (h *Handler) CreateVehicle(w http.ResponseWriter, r *http.Request) {
	var p vehiclePayload
	if err := json.NewDecoder(r.Body).Decode(&p); err != nil {
		webutil.BadRequest(w, "invalid JSON")
		return
	}
	if p.Name == nil || *p.Name == "" {
		webutil.BadRequest(w, "name is required")
		return
	}
	isActive := true
	if p.IsActive != nil {
		isActive = *p.IsActive
	}

	const q = `
        INSERT INTO vehicles (name, is_active)
        VALUES ($1, $2)
        RETURNING id, name, is_active;
    `
	var out vehicleDTO
	if err := h.DB.QueryRow(q, *p.Name, isActive).Scan(&out.ID, &out.Name, &out.IsActive); err != nil {
		webutil.ServerError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusCreated, out)
}

// GetVehicle gets a vehicle by ID.
func (h *Handler) GetVehicle(w http.ResponseWriter, r *http.Request) {
	id, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}

	const q = `SELECT id, name, is_active FROM vehicles WHERE id=$1;`
	var out vehicleDTO
	err := h.DB.QueryRow(q, id).Scan(&out.ID, &out.Name, &out.IsActive)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			http.NotFound(w, r)
			return
		}
		webutil.ServerError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, out)
}

// UpdateVehicle updates a vehicle's properties.
func (h *Handler) UpdateVehicle(w http.ResponseWriter, r *http.Request) {
	id, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}

	var p vehiclePayload
	if err := json.NewDecoder(r.Body).Decode(&p); err != nil {
		webutil.BadRequest(w, "invalid JSON")
		return
	}

	// Load current values
	const sel = `SELECT name, is_active FROM vehicles WHERE id=$1;`
	var curName string
	var curActive bool
	if err := h.DB.QueryRow(sel, id).Scan(&curName, &curActive); err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			http.NotFound(w, r)
			return
		}
		webutil.ServerError(w, err)
		return
	}

	if p.Name != nil {
		curName = *p.Name
	}
	if p.IsActive != nil {
		curActive = *p.IsActive
	}

	const upd = `
        UPDATE vehicles
        SET name=$1, is_active=$2, updated_at=now()
        WHERE id=$3
        RETURNING id, name, is_active;
    `
	var out vehicleDTO
	if err := h.DB.QueryRow(upd, curName, curActive, id).Scan(&out.ID, &out.Name, &out.IsActive); err != nil {
		webutil.ServerError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, out)
}

// DeleteVehicle deletes a vehicle by ID.
func (h *Handler) DeleteVehicle(w http.ResponseWriter, r *http.Request) {
	id, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}

	res, err := h.DB.Exec(`DELETE FROM vehicles WHERE id=$1;`, id)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	n, _ := res.RowsAffected()
	if n == 0 {
		http.NotFound(w, r)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

// VehicleHistory returns the maintenance history of a vehicle.
func (h *Handler) VehicleHistory(w http.ResponseWriter, r *http.Request) {
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
	var vehicleName string
	if err := h.DB.QueryRow(`SELECT name FROM vehicles WHERE id=$1`, vehicleID).Scan(&vehicleName); err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			http.NotFound(w, r)
			return
		}
		webutil.ServerError(w, err)
		return
	}
	airRows, err := h.DB.Query(`SELECT id,filled_at FROM vehicle_air_fills WHERE vehicle_id=$1 AND user_id=$2 ORDER BY filled_at DESC,id DESC`, vehicleID, user.ID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	defer airRows.Close()
	air := make([]airFillHistory, 0)
	for airRows.Next() {
		var item airFillHistory
		if err := airRows.Scan(&item.ID, &item.FilledAt); err != nil {
			webutil.ServerError(w, err)
			return
		}
		item.FilledAt = item.FilledAt.UTC()
		air = append(air, item)
	}
	fuelRows, err := h.DB.Query(`SELECT id,odometer_km,filled_at,station_name,notes FROM vehicle_fuel_fillups WHERE vehicle_id=$1 AND user_id=$2 ORDER BY filled_at DESC,id DESC`, vehicleID, user.ID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	defer fuelRows.Close()
	fuels := make([]fuelFillHistory, 0)
	for fuelRows.Next() {
		var fill fuelFillHistory
		if err := fuelRows.Scan(&fill.ID, &fill.OdometerKM, &fill.FilledAt, &fill.StationName, &fill.Notes); err != nil {
			webutil.ServerError(w, err)
			return
		}
		fill.FilledAt = fill.FilledAt.UTC()
		rows, err := h.DB.Query(`SELECT fuel_type,fill_type,quantity,unit_price,total_cost FROM vehicle_fuel_items WHERE fillup_id=$1 ORDER BY id`, fill.ID)
		if err != nil {
			webutil.ServerError(w, err)
			return
		}
		fill.Items = []fuelHistoryItem{}
		for rows.Next() {
			var item fuelHistoryItem
			if err := rows.Scan(&item.FuelType, &item.FillType, &item.Quantity, &item.UnitPrice, &item.TotalCost); err != nil {
				rows.Close()
				webutil.ServerError(w, err)
				return
			}
			fill.Items = append(fill.Items, item)
		}
		rows.Close()
		fuels = append(fuels, fill)
	}
	webutil.WriteJSON(w, http.StatusOK, map[string]any{"vehicle_name": vehicleName, "air_fills": air, "fuel_fillups": fuels, "average_mileage_km_per_litre": h.averageFuelEconomies(vehicleID, user.ID)})
}

// DeleteVehicleAirFill deletes an air fill record.
func (h *Handler) DeleteVehicleAirFill(w http.ResponseWriter, r *http.Request) {
	h.deleteVehicleRecord(w, r, "vehicle_air_fills", "airFillID")
}

// DeleteFuelFillup deletes a fuel fillup record.
func (h *Handler) DeleteFuelFillup(w http.ResponseWriter, r *http.Request) {
	h.deleteVehicleRecord(w, r, "vehicle_fuel_fillups", "fillupID")
}

func (h *Handler) deleteVehicleRecord(w http.ResponseWriter, r *http.Request, table, param string) {
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
	recordID, err := strconv.ParseInt(chi.URLParam(r, param), 10, 64)
	if err != nil || recordID <= 0 {
		webutil.BadRequest(w, "invalid record id")
		return
	}
	result, err := h.DB.Exec(`DELETE FROM `+table+` WHERE id=$1 AND vehicle_id=$2 AND user_id=$3`, recordID, vehicleID, user.ID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	n, err := result.RowsAffected()
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	if n == 0 {
		http.NotFound(w, r)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
