package vehicle

import (
	"database/sql"
	"encoding/json"
	"errors"
	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/jackc/pgx/v5/pgtype"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/go-chi/chi/v5"

	"github.com/DarkAbhi/life-backend/internal/vehicle/query"
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
	ID           int64                   `json:"id"`
	Category     string                  `json:"category"`
	Title        string                  `json:"title"`
	Amount       float64                 `json:"amount"`
	OccurredAt   time.Time               `json:"occurred_at"`
	OdometerKM   *float64                `json:"odometer_km"`
	ProviderName *string                 `json:"provider_name"`
	Notes        *string                 `json:"notes"`
	Attachments  []maintenanceAttachment `json:"attachments"`
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

func nullableString(value *string) sql.NullString {
	if value == nil {
		return sql.NullString{}
	}
	return sql.NullString{String: *value, Valid: true}
}

func maintenanceRecordDTO(id int64, category, title string, amount float64, occurredAt time.Time, odometer *float64, provider, notes sql.NullString) maintenanceRecord {
	record := maintenanceRecord{ID: id, Category: category, Title: title, Amount: amount, OccurredAt: occurredAt.UTC(), OdometerKM: odometer}
	if provider.Valid {
		record.ProviderName = &provider.String
	}
	if notes.Valid {
		record.Notes = &notes.String
	}
	return record
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
		webutil.BadRequest(w, "invalid json")
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
	row, err := query.New(h.DB).CreateMaintenanceRecord(r.Context(), query.CreateMaintenanceRecordParams{VehicleID: vehicleID, UserID: user.ID, Category: p.Category, Title: p.Title, Amount: p.Amount, OccurredAt: pgtype.Timestamptz{Time: occurredAt, Valid: true}, OdometerKm: p.OdometerKM, ProviderName: nullableString(p.ProviderName), Notes: nullableText(p.Notes)})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	record := maintenanceRecordDTO(row.ID, row.Category, row.Title, row.Amount, row.OccurredAt.Time, row.OdometerKm, row.ProviderName, textToNullString(row.Notes))
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
		webutil.BadRequest(w, "invalid json")
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
	row, err := query.New(h.DB).UpdateMaintenanceRecord(r.Context(), query.UpdateMaintenanceRecordParams{Category: p.Category, Title: p.Title, Amount: p.Amount, OccurredAt: pgtype.Timestamptz{Time: occurredAt, Valid: true}, OdometerKm: p.OdometerKM, ProviderName: nullableString(p.ProviderName), Notes: nullableText(p.Notes), ID: recordID, VehicleID: vehicleID, UserID: user.ID})
	if errors.Is(err, sql.ErrNoRows) {
		http.NotFound(w, r)
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	record := maintenanceRecordDTO(row.ID, row.Category, row.Title, row.Amount, row.OccurredAt.Time, row.OdometerKm, row.ProviderName, textToNullString(row.Notes))
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
	if err := h.deleteMaintenanceAttachmentObjects(r.Context(), recordID, user.ID); err != nil {
		webutil.ServerError(w, err)
		return
	}
	count, err := query.New(h.DB).DeleteMaintenanceRecord(r.Context(), query.DeleteMaintenanceRecordParams{ID: recordID, VehicleID: vehicleID, UserID: user.ID})
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
	user, err := h.sessionUser(r)
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

func nullableText(value *string) pgtype.Text {
	if value == nil {
		return pgtype.Text{}
	}
	return pgtype.Text{String: *value, Valid: true}
}

func textToNullString(value pgtype.Text) sql.NullString {
	return sql.NullString{String: value.String, Valid: value.Valid}
}
