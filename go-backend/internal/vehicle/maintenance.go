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

var maintenanceCategories = map[string]bool{
	"service": true, "repair": true, "insurance": true, "washing": true, "tyres": true,
}

type maintenanceRecordPayload struct {
	Category     string     `json:"category"`
	Title        string     `json:"title"`
	Amount       float64    `json:"amount"`
	OccurredAt   *time.Time `json:"occurred_at"`
	OdometerKM   *float64   `json:"odometer_km"`
	ProviderName *string    `json:"provider_name"`
	Notes        *string    `json:"notes"`
}

type maintenanceRecord struct {
	ID           int64     `json:"id"`
	Category     string    `json:"category"`
	Title        string    `json:"title"`
	Amount       float64   `json:"amount"`
	OccurredAt   time.Time `json:"occurred_at"`
	OdometerKM   *float64  `json:"odometer_km"`
	ProviderName *string   `json:"provider_name"`
	Notes        *string   `json:"notes"`
}

func normalizeMaintenanceRecord(p *maintenanceRecordPayload) error {
	p.Category = strings.ToLower(strings.TrimSpace(p.Category))
	p.Title = strings.TrimSpace(p.Title)
	if !maintenanceCategories[p.Category] {
		return errors.New("invalid maintenance category")
	}
	if p.Title == "" || len(p.Title) > 160 {
		return errors.New("title is required and must be 160 characters or fewer")
	}
	if p.Amount <= 0 {
		return errors.New("amount must be greater than zero")
	}
	if p.OdometerKM != nil && *p.OdometerKM < 0 {
		return errors.New("odometer cannot be negative")
	}
	if p.ProviderName != nil {
		value := strings.TrimSpace(*p.ProviderName)
		if len(value) > 160 {
			return errors.New("provider name must be 160 characters or fewer")
		}
		p.ProviderName = optionalString(value)
	}
	if p.Notes != nil {
		p.Notes = optionalString(strings.TrimSpace(*p.Notes))
	}
	return nil
}

func optionalString(value string) *string {
	if value == "" {
		return nil
	}
	return &value
}

func (h *Handler) CreateMaintenanceRecord(w http.ResponseWriter, r *http.Request) {
	user, ok := h.maintenanceUser(w, r)
	if !ok {
		return
	}
	vehicleID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	var p maintenanceRecordPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<20)).Decode(&p); err != nil {
		webutil.BadRequest(w, "invalid JSON")
		return
	}
	if err := normalizeMaintenanceRecord(&p); err != nil {
		webutil.BadRequest(w, err.Error())
		return
	}
	occurredAt := time.Now().UTC()
	if p.OccurredAt != nil {
		occurredAt = *p.OccurredAt
	}
	var record maintenanceRecord
	err := h.DB.QueryRow(`INSERT INTO vehicle_maintenance_records (vehicle_id,user_id,category,title,amount,occurred_at,odometer_km,provider_name,notes) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9) RETURNING id,category,title,amount,occurred_at,odometer_km,provider_name,notes`, vehicleID, user.ID, p.Category, p.Title, p.Amount, occurredAt, p.OdometerKM, p.ProviderName, p.Notes).Scan(&record.ID, &record.Category, &record.Title, &record.Amount, &record.OccurredAt, &record.OdometerKM, &record.ProviderName, &record.Notes)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	record.OccurredAt = record.OccurredAt.UTC()
	webutil.WriteJSON(w, http.StatusCreated, record)
}

func (h *Handler) UpdateMaintenanceRecord(w http.ResponseWriter, r *http.Request) {
	user, ok := h.maintenanceUser(w, r)
	if !ok {
		return
	}
	vehicleID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	recordID, err := strconv.ParseInt(chi.URLParam(r, "recordID"), 10, 64)
	if err != nil || recordID <= 0 {
		webutil.BadRequest(w, "invalid maintenance record id")
		return
	}
	var p maintenanceRecordPayload
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<20)).Decode(&p); err != nil {
		webutil.BadRequest(w, "invalid JSON")
		return
	}
	if err := normalizeMaintenanceRecord(&p); err != nil {
		webutil.BadRequest(w, err.Error())
		return
	}
	occurredAt := time.Now().UTC()
	if p.OccurredAt != nil {
		occurredAt = *p.OccurredAt
	}
	var record maintenanceRecord
	err = h.DB.QueryRow(`UPDATE vehicle_maintenance_records SET category=$1,title=$2,amount=$3,occurred_at=$4,odometer_km=$5,provider_name=$6,notes=$7,updated_at=CURRENT_TIMESTAMP WHERE id=$8 AND vehicle_id=$9 AND user_id=$10 RETURNING id,category,title,amount,occurred_at,odometer_km,provider_name,notes`, p.Category, p.Title, p.Amount, occurredAt, p.OdometerKM, p.ProviderName, p.Notes, recordID, vehicleID, user.ID).Scan(&record.ID, &record.Category, &record.Title, &record.Amount, &record.OccurredAt, &record.OdometerKM, &record.ProviderName, &record.Notes)
	if errors.Is(err, sql.ErrNoRows) {
		http.NotFound(w, r)
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	record.OccurredAt = record.OccurredAt.UTC()
	webutil.WriteJSON(w, http.StatusOK, record)
}

func (h *Handler) DeleteMaintenanceRecord(w http.ResponseWriter, r *http.Request) {
	user, ok := h.maintenanceUser(w, r)
	if !ok {
		return
	}
	vehicleID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	recordID, err := strconv.ParseInt(chi.URLParam(r, "recordID"), 10, 64)
	if err != nil || recordID <= 0 {
		webutil.BadRequest(w, "invalid maintenance record id")
		return
	}
	result, err := h.DB.Exec(`DELETE FROM vehicle_maintenance_records WHERE id=$1 AND vehicle_id=$2 AND user_id=$3`, recordID, vehicleID, user.ID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	count, err := result.RowsAffected()
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	if count == 0 {
		http.NotFound(w, r)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) maintenanceUser(w http.ResponseWriter, r *http.Request) (auth.SessionUser, bool) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return auth.SessionUser{}, false
	}
	if err != nil {
		webutil.ServerError(w, err)
		return auth.SessionUser{}, false
	}
	return user, true
}
