// Package vehicle manages vehicles, fuel, tire pressure, maintenance, and attachments.
package vehicle

import (
	"database/sql"
	"encoding/json"
	"errors"
	"github.com/jackc/pgx/v5/pgxpool"
	"net/http"
	"strconv"
	"time"

	"github.com/go-chi/chi/v5"

	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/DarkAbhi/life-backend/internal/vehicle/query"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type payload struct {
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

type DTO struct {
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
	DB          *pgxpool.Pool
	attachments *attachmentStorage
	service     *Service
	sessions    SessionLookup
}

func NewHandler(db *pgxpool.Pool, service *Service, sessions SessionLookup) *Handler {
	return &Handler{DB: db, service: service, sessions: sessions}
}

type SessionLookup func(*http.Request) (auth.SessionUser, error)

func (h *Handler) sessionUser(r *http.Request) (auth.SessionUser, error) { return h.sessions(r) }

func dtoFromFields(id int64, name string, isActive bool, frontSolo, rearSolo, frontPillion, rearPillion *float64) DTO {
	return DTO{ID: id, Name: name, IsActive: isActive, FrontTirePressureSolo: frontSolo, RearTirePressureSolo: rearSolo, FrontTirePressurePillion: frontPillion, RearTirePressurePillion: rearPillion, FrontTirePressure: frontSolo, RearTirePressure: rearSolo}
}

// List lists all vehicles.
func (h *Handler) List(w http.ResponseWriter, r *http.Request) {
	rows, err := h.service.List(r.Context())
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	out := make([]struct {
		ID   int64  `json:"id"`
		Name string `json:"name"`
	}, 0, len(rows))
	for _, row := range rows {
		out = append(out, struct {
			ID   int64  `json:"id"`
			Name string `json:"name"`
		}{row.ID, row.Name})
	}
	webutil.WriteJSON(w, http.StatusOK, out)
}

// Create creates a new vehicle.
func (h *Handler) Create(w http.ResponseWriter, r *http.Request) {
	var p payload
	if err := json.NewDecoder(r.Body).Decode(&p); err != nil {
		webutil.BadRequest(w, "invalid json")
		return
	}
	row, err := h.service.Create(r.Context(), Changes(p))
	if h.writeError(w, r, err) {
		return
	}
	webutil.WriteJSON(w, http.StatusCreated, dtoFromFields(row.ID, row.Name, row.IsActive, row.FrontTirePressureSolo, row.RearTirePressureSolo, row.FrontTirePressurePillion, row.RearTirePressurePillion))
}

// Show gets a vehicle by ID.
func (h *Handler) Show(w http.ResponseWriter, r *http.Request) {
	id, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	row, err := h.service.Fetch(r.Context(), id)
	if h.writeError(w, r, err) {
		return
	}
	webutil.WriteJSON(w, http.StatusOK, dtoFromFields(row.ID, row.Name, row.IsActive, row.FrontTirePressureSolo, row.RearTirePressureSolo, row.FrontTirePressurePillion, row.RearTirePressurePillion))
}

// Update updates a vehicle's properties.
func (h *Handler) Update(w http.ResponseWriter, r *http.Request) {
	id, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	var p payload
	if err := json.NewDecoder(r.Body).Decode(&p); err != nil {
		webutil.BadRequest(w, "invalid json")
		return
	}
	row, err := h.service.Update(r.Context(), id, Changes(p))
	if h.writeError(w, r, err) {
		return
	}
	webutil.WriteJSON(w, http.StatusOK, dtoFromFields(row.ID, row.Name, row.IsActive, row.FrontTirePressureSolo, row.RearTirePressureSolo, row.FrontTirePressurePillion, row.RearTirePressurePillion))
}

// UpdateTirePressure updates the solo and pillion tire pressures for a vehicle.
func (h *Handler) UpdateTirePressure(w http.ResponseWriter, r *http.Request) {
	_, err := h.sessionUser(r)
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
		webutil.BadRequest(w, "invalid json")
		return
	}
	row, err := h.service.UpdatePressure(r.Context(), id, PressureChanges(p))
	if h.writeError(w, r, err) {
		return
	}
	webutil.WriteJSON(w, http.StatusOK, dtoFromFields(row.ID, row.Name, row.IsActive, row.FrontTirePressureSolo, row.RearTirePressureSolo, row.FrontTirePressurePillion, row.RearTirePressurePillion))
}

// Delete deletes a vehicle by ID.
func (h *Handler) Delete(w http.ResponseWriter, r *http.Request) {
	id, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	if h.writeError(w, r, h.service.Delete(r.Context(), id)) {
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) writeError(w http.ResponseWriter, r *http.Request, err error) bool {
	if err == nil {
		return false
	}
	var validation ValidationError
	switch {
	case errors.As(err, &validation):
		webutil.BadRequest(w, validation.Message)
	case errors.Is(err, ErrNotFound):
		http.NotFound(w, r)
	default:
		webutil.ServerError(w, err)
	}
	return true
}

// History returns the maintenance history of a vehicle.
func (h *Handler) History(w http.ResponseWriter, r *http.Request) {
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
	header, err := q.GetVehicleHistoryHeader(r.Context(), vehicleID)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			http.NotFound(w, r)
			return
		}
		webutil.ServerError(w, err)
		return
	}
	airRows, err := q.ListVehicleAirFills(r.Context(), query.ListVehicleAirFillsParams{VehicleID: vehicleID, UserID: user.ID})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	air := make([]airFillHistory, 0)
	for _, row := range airRows {
		item := airFillHistory{ID: row.ID, FilledAt: row.FilledAt.Time.UTC()}
		air = append(air, item)
	}
	fuelRows, err := q.ListVehicleFuelFillups(r.Context(), query.ListVehicleFuelFillupsParams{VehicleID: vehicleID, UserID: user.ID})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	fuels := make([]fuelFillHistory, 0)
	for _, row := range fuelRows {
		fill := fuelFillHistory{ID: row.ID, OdometerKM: row.OdometerKm, FilledAt: row.FilledAt.Time.UTC()}
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
	maintenanceRows, err := q.ListVehicleMaintenanceRecords(r.Context(), query.ListVehicleMaintenanceRecordsParams{VehicleID: vehicleID, UserID: user.ID})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	maintenance := make([]maintenanceRecord, 0)
	for _, row := range maintenanceRows {
		record := maintenanceRecordDTO(row.ID, row.Category, row.Title, row.Amount, row.OccurredAt.Time, row.OdometerKm, row.ProviderName, textToNullString(row.Notes))
		attachmentRows, err := q.ListMaintenanceAttachments(r.Context(), query.ListMaintenanceAttachmentsParams{MaintenanceRecordID: record.ID, UserID: user.ID})
		if err != nil {
			webutil.ServerError(w, err)
			return
		}
		record.Attachments = []maintenanceAttachment{}
		for _, row := range attachmentRows {
			attachment := maintenanceAttachment{ID: row.ID, FileName: row.FileName, ContentType: row.ContentType, SizeBytes: row.SizeBytes, CreatedAt: row.CreatedAt.Time.UTC()}
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

// DeleteAirFill deletes an air fill record.
func (h *Handler) DeleteAirFill(w http.ResponseWriter, r *http.Request) {
	h.deleteRecord(w, r, false, "airFillID")
}

// DeleteFuelFillup deletes a fuel fillup record.
func (h *Handler) DeleteFuelFillup(w http.ResponseWriter, r *http.Request) {
	h.deleteRecord(w, r, true, "fillupID")
}

func (h *Handler) deleteRecord(w http.ResponseWriter, r *http.Request, isFuel bool, param string) {
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
	recordID, err := strconv.ParseInt(chi.URLParam(r, param), 10, 64)
	if err != nil || recordID <= 0 {
		webutil.BadRequest(w, "invalid record id")
		return
	}
	q := query.New(h.DB)
	var n int64
	if isFuel {
		n, err = q.DeleteVehicleFuelFillup(r.Context(), query.DeleteVehicleFuelFillupParams{ID: recordID, VehicleID: vehicleID, UserID: user.ID})
	} else {
		n, err = q.DeleteVehicleAirFill(r.Context(), query.DeleteVehicleAirFillParams{ID: recordID, VehicleID: vehicleID, UserID: user.ID})
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
