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
	"github.com/DarkAbhi/life-backend/internal/db/sqlc"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type vehiclePayload struct {
	Name                     *string  `json:"name"`
	IsActive                 *bool    `json:"is_active"`
	FrontTirePressureSolo    *float64 `json:"front_tire_pressure_solo"`
	RearTirePressureSolo     *float64 `json:"rear_tire_pressure_solo"`
	FrontTirePressurePillion *float64 `json:"front_tire_pressure_pillion"`
	RearTirePressurePillion  *float64 `json:"rear_tire_pressure_pillion"`
	FrontTirePressure        *float64 `json:"front_tire_pressure"`
	RearTirePressure         *float64 `json:"rear_tire_pressure"`
}

type tirePressurePayload struct {
	FrontTirePressureSolo    *float64 `json:"front_tire_pressure_solo"`
	RearTirePressureSolo     *float64 `json:"rear_tire_pressure_solo"`
	FrontTirePressurePillion *float64 `json:"front_tire_pressure_pillion"`
	RearTirePressurePillion  *float64 `json:"rear_tire_pressure_pillion"`
	FrontTirePressure        *float64 `json:"front_tire_pressure"`
	RearTirePressure         *float64 `json:"rear_tire_pressure"`
}

type vehicleDTO struct {
	ID                       int64    `json:"id"`
	Name                     string   `json:"name"`
	IsActive                 bool     `json:"is_active"`
	FrontTirePressureSolo    *float64 `json:"front_tire_pressure_solo"`
	RearTirePressureSolo     *float64 `json:"rear_tire_pressure_solo"`
	FrontTirePressurePillion *float64 `json:"front_tire_pressure_pillion"`
	RearTirePressurePillion  *float64 `json:"rear_tire_pressure_pillion"`
	FrontTirePressure        *float64 `json:"front_tire_pressure,omitempty"`
	RearTirePressure         *float64 `json:"rear_tire_pressure,omitempty"`
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
	DB          *sql.DB
	attachments *attachmentStorage
}

func NewHandler(db *sql.DB) *Handler {
	return &Handler{DB: db}
}

func vehicleDTOFromFields(id int64, name string, active bool, frontSolo, rearSolo, frontPillion, rearPillion *float64) vehicleDTO {
	return vehicleDTO{ID: id, Name: name, IsActive: active, FrontTirePressureSolo: frontSolo, RearTirePressureSolo: rearSolo, FrontTirePressurePillion: frontPillion, RearTirePressurePillion: rearPillion, FrontTirePressure: frontSolo, RearTirePressure: rearSolo}
}

// ListVehicles lists all vehicles.
func (h *Handler) ListVehicles(w http.ResponseWriter, r *http.Request) {
	rows, err := sqlc.New(h.DB).ListVehicles(r.Context())
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	type vehicleItem struct {
		ID   int64  `json:"id"`
		Name string `json:"name"`
	}
	out := make([]vehicleItem, 0)
	for _, row := range rows {
		v := vehicleItem{ID: row.ID, Name: row.Name}
		out = append(out, v)
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

	frontSolo := p.FrontTirePressureSolo
	if frontSolo == nil && p.FrontTirePressure != nil {
		frontSolo = p.FrontTirePressure
	}
	rearSolo := p.RearTirePressureSolo
	if rearSolo == nil && p.RearTirePressure != nil {
		rearSolo = p.RearTirePressure
	}
	frontPillion := p.FrontTirePressurePillion
	rearPillion := p.RearTirePressurePillion

	if frontSolo != nil && *frontSolo < 0 {
		webutil.BadRequest(w, "front solo tire pressure cannot be negative")
		return
	}
	if rearSolo != nil && *rearSolo < 0 {
		webutil.BadRequest(w, "rear solo tire pressure cannot be negative")
		return
	}
	if frontPillion != nil && *frontPillion < 0 {
		webutil.BadRequest(w, "front pillion tire pressure cannot be negative")
		return
	}
	if rearPillion != nil && *rearPillion < 0 {
		webutil.BadRequest(w, "rear pillion tire pressure cannot be negative")
		return
	}

	row, err := sqlc.New(h.DB).CreateVehicle(r.Context(), sqlc.CreateVehicleParams{Name: *p.Name, IsActive: isActive, FrontTirePressureSolo: frontSolo, RearTirePressureSolo: rearSolo, FrontTirePressurePillion: frontPillion, RearTirePressurePillion: rearPillion})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	out := vehicleDTOFromFields(row.ID, row.Name, row.IsActive, row.FrontTirePressureSolo, row.RearTirePressureSolo, row.FrontTirePressurePillion, row.RearTirePressurePillion)
	webutil.WriteJSON(w, http.StatusCreated, out)
}

// GetVehicle gets a vehicle by ID.
func (h *Handler) GetVehicle(w http.ResponseWriter, r *http.Request) {
	id, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}

	row, err := sqlc.New(h.DB).GetVehicle(r.Context(), id)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			http.NotFound(w, r)
			return
		}
		webutil.ServerError(w, err)
		return
	}
	out := vehicleDTOFromFields(row.ID, row.Name, row.IsActive, row.FrontTirePressureSolo, row.RearTirePressureSolo, row.FrontTirePressurePillion, row.RearTirePressurePillion)
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

	current, err := sqlc.New(h.DB).GetVehicleForUpdate(r.Context(), id)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			http.NotFound(w, r)
			return
		}
		webutil.ServerError(w, err)
		return
	}
	curName, curActive := current.Name, current.IsActive
	curFrontSolo, curRearSolo := current.FrontTirePressureSolo, current.RearTirePressureSolo
	curFrontPillion, curRearPillion := current.FrontTirePressurePillion, current.RearTirePressurePillion

	if p.Name != nil {
		curName = *p.Name
	}
	if p.IsActive != nil {
		curActive = *p.IsActive
	}

	frontSolo := p.FrontTirePressureSolo
	if frontSolo == nil && p.FrontTirePressure != nil {
		frontSolo = p.FrontTirePressure
	}
	if frontSolo != nil {
		if *frontSolo < 0 {
			webutil.BadRequest(w, "front solo tire pressure cannot be negative")
			return
		}
		curFrontSolo = frontSolo
	}

	rearSolo := p.RearTirePressureSolo
	if rearSolo == nil && p.RearTirePressure != nil {
		rearSolo = p.RearTirePressure
	}
	if rearSolo != nil {
		if *rearSolo < 0 {
			webutil.BadRequest(w, "rear solo tire pressure cannot be negative")
			return
		}
		curRearSolo = rearSolo
	}

	if p.FrontTirePressurePillion != nil {
		if *p.FrontTirePressurePillion < 0 {
			webutil.BadRequest(w, "front pillion tire pressure cannot be negative")
			return
		}
		curFrontPillion = p.FrontTirePressurePillion
	}
	if p.RearTirePressurePillion != nil {
		if *p.RearTirePressurePillion < 0 {
			webutil.BadRequest(w, "rear pillion tire pressure cannot be negative")
			return
		}
		curRearPillion = p.RearTirePressurePillion
	}

	row, err := sqlc.New(h.DB).UpdateVehicle(r.Context(), sqlc.UpdateVehicleParams{Name: curName, IsActive: curActive, FrontTirePressureSolo: curFrontSolo, RearTirePressureSolo: curRearSolo, FrontTirePressurePillion: curFrontPillion, RearTirePressurePillion: curRearPillion, ID: id})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	out := vehicleDTOFromFields(row.ID, row.Name, row.IsActive, row.FrontTirePressureSolo, row.RearTirePressureSolo, row.FrontTirePressurePillion, row.RearTirePressurePillion)
	webutil.WriteJSON(w, http.StatusOK, out)
}

// UpdateVehicleTirePressure updates the solo and pillion tire pressures for a vehicle.
func (h *Handler) UpdateVehicleTirePressure(w http.ResponseWriter, r *http.Request) {
	_, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	id, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}

	var p tirePressurePayload
	if err := json.NewDecoder(r.Body).Decode(&p); err != nil {
		webutil.BadRequest(w, "invalid JSON")
		return
	}

	frontSolo := p.FrontTirePressureSolo
	if frontSolo == nil && p.FrontTirePressure != nil {
		frontSolo = p.FrontTirePressure
	}
	rearSolo := p.RearTirePressureSolo
	if rearSolo == nil && p.RearTirePressure != nil {
		rearSolo = p.RearTirePressure
	}
	frontPillion := p.FrontTirePressurePillion
	rearPillion := p.RearTirePressurePillion

	if frontSolo != nil && *frontSolo < 0 {
		webutil.BadRequest(w, "front solo tire pressure cannot be negative")
		return
	}
	if rearSolo != nil && *rearSolo < 0 {
		webutil.BadRequest(w, "rear solo tire pressure cannot be negative")
		return
	}
	if frontPillion != nil && *frontPillion < 0 {
		webutil.BadRequest(w, "front pillion tire pressure cannot be negative")
		return
	}
	if rearPillion != nil && *rearPillion < 0 {
		webutil.BadRequest(w, "rear pillion tire pressure cannot be negative")
		return
	}

	row, err := sqlc.New(h.DB).UpdateVehicleTirePressure(r.Context(), sqlc.UpdateVehicleTirePressureParams{FrontTirePressureSolo: frontSolo, RearTirePressureSolo: rearSolo, FrontTirePressurePillion: frontPillion, RearTirePressurePillion: rearPillion, ID: id})
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			http.NotFound(w, r)
			return
		}
		webutil.ServerError(w, err)
		return
	}
	out := vehicleDTOFromFields(row.ID, row.Name, row.IsActive, row.FrontTirePressureSolo, row.RearTirePressureSolo, row.FrontTirePressurePillion, row.RearTirePressurePillion)
	webutil.WriteJSON(w, http.StatusOK, out)
}

// DeleteVehicle deletes a vehicle by ID.
func (h *Handler) DeleteVehicle(w http.ResponseWriter, r *http.Request) {
	id, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}

	n, err := sqlc.New(h.DB).DeleteVehicle(r.Context(), id)
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
	q := sqlc.New(h.DB)
	header, err := q.GetVehicleHistoryHeader(r.Context(), vehicleID)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			http.NotFound(w, r)
			return
		}
		webutil.ServerError(w, err)
		return
	}
	airRows, err := q.ListVehicleAirFills(r.Context(), sqlc.ListVehicleAirFillsParams{VehicleID: vehicleID, UserID: user.ID})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	air := make([]airFillHistory, 0)
	for _, row := range airRows {
		item := airFillHistory{ID: row.ID, FilledAt: row.FilledAt.UTC()}
		air = append(air, item)
	}
	fuelRows, err := q.ListVehicleFuelFillups(r.Context(), sqlc.ListVehicleFuelFillupsParams{VehicleID: vehicleID, UserID: user.ID})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	fuels := make([]fuelFillHistory, 0)
	for _, row := range fuelRows {
		fill := fuelFillHistory{ID: row.ID, OdometerKM: row.OdometerKm, FilledAt: row.FilledAt.UTC()}
		if row.StationName.Valid {
			fill.StationName = &row.StationName.String
		}
		if row.Notes.Valid {
			fill.Notes = &row.Notes.String
		}
		rows, err := q.ListFuelItems(r.Context(), fill.ID)
		if err != nil {
			webutil.ServerError(w, err)
			return
		}
		fill.Items = []fuelHistoryItem{}
		for _, row := range rows {
			item := fuelHistoryItem{FuelType: row.FuelType, FillType: row.FillType, Quantity: row.Quantity, UnitPrice: row.UnitPrice, TotalCost: row.TotalCost}
			fill.Items = append(fill.Items, item)
		}
		fuels = append(fuels, fill)
	}
	maintenanceRows, err := q.ListVehicleMaintenanceRecords(r.Context(), sqlc.ListVehicleMaintenanceRecordsParams{VehicleID: vehicleID, UserID: user.ID})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	maintenance := make([]maintenanceRecord, 0)
	for _, row := range maintenanceRows {
		record := maintenanceRecordDTO(row.ID, row.Category, row.Title, row.Amount, row.OccurredAt, row.OdometerKm, row.ProviderName, row.Notes)
		attachmentRows, err := q.ListMaintenanceAttachments(r.Context(), sqlc.ListMaintenanceAttachmentsParams{MaintenanceRecordID: record.ID, UserID: user.ID})
		if err != nil {
			webutil.ServerError(w, err)
			return
		}
		record.Attachments = []maintenanceAttachment{}
		for _, row := range attachmentRows {
			attachment := maintenanceAttachment{ID: row.ID, FileName: row.FileName, ContentType: row.ContentType, SizeBytes: row.SizeBytes, CreatedAt: row.CreatedAt.UTC()}
			record.Attachments = append(record.Attachments, attachment)
		}
		maintenance = append(maintenance, record)
	}
	webutil.WriteJSON(w, http.StatusOK, map[string]any{
		"vehicle_name":                 header.Name,
		"front_tire_pressure_solo":     header.FrontTirePressureSolo,
		"rear_tire_pressure_solo":      header.RearTirePressureSolo,
		"front_tire_pressure_pillion":  header.FrontTirePressurePillion,
		"rear_tire_pressure_pillion":   header.RearTirePressurePillion,
		"front_tire_pressure":          header.FrontTirePressureSolo,
		"rear_tire_pressure":           header.RearTirePressureSolo,
		"air_fills":                    air,
		"fuel_fillups":                 fuels,
		"maintenance_records":          maintenance,
		"average_mileage_km_per_litre": h.averageFuelEconomies(vehicleID, user.ID),
	})
}

// DeleteVehicleAirFill deletes an air fill record.
func (h *Handler) DeleteVehicleAirFill(w http.ResponseWriter, r *http.Request) {
	h.deleteVehicleRecord(w, r, false, "airFillID")
}

// DeleteFuelFillup deletes a fuel fillup record.
func (h *Handler) DeleteFuelFillup(w http.ResponseWriter, r *http.Request) {
	h.deleteVehicleRecord(w, r, true, "fillupID")
}

func (h *Handler) deleteVehicleRecord(w http.ResponseWriter, r *http.Request, fuel bool, param string) {
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
	q := sqlc.New(h.DB)
	var n int64
	if fuel {
		n, err = q.DeleteVehicleFuelFillup(r.Context(), sqlc.DeleteVehicleFuelFillupParams{ID: recordID, VehicleID: vehicleID, UserID: user.ID})
	} else {
		n, err = q.DeleteVehicleAirFill(r.Context(), sqlc.DeleteVehicleAirFillParams{ID: recordID, VehicleID: vehicleID, UserID: user.ID})
	}
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
