package vehicle

import (
	"context"
	"encoding/json"
	"errors"
	"github.com/DarkAbhi/life-backend/internal/vehicle/query"
	"github.com/DarkAbhi/life-backend/internal/webutil"
	"github.com/go-chi/chi/v5"
	"net/http"
	"strings"
)

type scheduleInterval struct {
	Kilometers int32  `json:"kilometers"`
	Duration   int32  `json:"duration"`
	Unit       string `json:"unit"`
}
type maintenanceSchedule struct {
	Kind            string           `json:"kind"`
	ActiveSource    string           `json:"active_source"`
	Personal        scheduleInterval `json:"personal"`
	Manual          scheduleInterval `json:"manual"`
	ManualReference string           `json:"manual_reference"`
}

func startingSchedules(kind string) []maintenanceSchedule {
	oil, service := scheduleInterval{10000, 9, "months"}, scheduleInterval{10000, 12, "months"}
	pressure := scheduleInterval{0, 30, "days"}
	switch kind {
	case "motorcycle", "moped":
		oil, service, pressure = scheduleInterval{3000, 4, "months"}, scheduleInterval{5000, 6, "months"}, scheduleInterval{0, 14, "days"}
	case "car":
	default:
		oil, service = scheduleInterval{Unit: "months"}, scheduleInterval{Unit: "months"}
	}
	result := []maintenanceSchedule{}
	for i, key := range []string{"engine_oil", "tire_pressure", "general_service"} {
		result = append(result, maintenanceSchedule{Kind: key, ActiveSource: "personal", Personal: []scheduleInterval{oil, pressure, service}[i], Manual: scheduleInterval{Unit: "months"}})
	}
	return result
}
func (s maintenanceSchedule) effective() scheduleInterval {
	if s.ActiveSource == "manual" {
		return s.Manual
	}
	return s.Personal
}
func normalizeSchedule(s *maintenanceSchedule) error {
	if s.Kind != "engine_oil" && s.Kind != "general_service" && s.Kind != "tire_pressure" {
		return errors.New("unsupported maintenance task")
	}
	if s.ActiveSource != "personal" && s.ActiveSource != "manual" {
		return errors.New("choose an active schedule")
	}
	for _, v := range []*scheduleInterval{&s.Personal, &s.Manual} {
		if v.Unit == "" {
			v.Unit = "months"
		}
		limit := int32(1200)
		if v.Unit == "days" {
			limit = 36500
		}
		if (v.Unit != "months" && v.Unit != "days") || v.Duration < 0 || v.Duration > limit || v.Kilometers < 0 || v.Kilometers > 1000000 {
			return errors.New("intervals must be positive whole numbers: up to 1,000,000 km, 1,200 months or 36,500 days; leave unused thresholds blank")
		}
	}
	v := s.effective()
	if v.Duration == 0 && v.Kilometers == 0 {
		return errors.New("the active schedule needs a distance or time interval")
	}
	s.ManualReference = strings.TrimSpace(s.ManualReference)
	if len(s.ManualReference) > 160 {
		return errors.New("manual reference must be at most 160 bytes")
	}
	return nil
}
func loadMaintenanceSchedules(ctx context.Context, q *query.Queries, vehicleID, userID int64, kind string) ([]maintenanceSchedule, error) {
	result := startingSchedules(kind)
	rows, err := q.ListMaintenanceSchedules(ctx, query.ListMaintenanceSchedulesParams{VehicleID: vehicleID, UserID: userID})
	if err != nil {
		return nil, err
	}
	for _, row := range rows {
		for i := range result {
			s := &result[i]
			if s.Kind == row.Kind {
				s.ActiveSource = row.ActiveSource
				s.Personal = scheduleInterval{row.PersonalKm, row.PersonalDuration, row.PersonalUnit}
				s.Manual = scheduleInterval{row.ManualKm, row.ManualDuration, row.ManualUnit}
				s.ManualReference = row.ManualReference
			}
		}
	}
	return result, nil
}
func (h *Handler) SaveMaintenanceSchedule(w http.ResponseWriter, r *http.Request) {
	user, ok := h.maintenanceUser(w, r)
	if !ok {
		return
	}
	vehicleID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	var s maintenanceSchedule
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 16<<10)).Decode(&s); err != nil {
		webutil.BadRequest(w, "invalid schedule")
		return
	}
	s.Kind = chi.URLParam(r, "kind")
	if s.ActiveSource == "default" {
		s.ActiveSource = "personal"
		if s.Personal.Kilometers == 0 && s.Personal.Duration == 0 {
			header, err := query.New(h.DB).GetVehicleHistoryHeader(r.Context(), query.GetVehicleHistoryHeaderParams{ID: vehicleID, UserID: user.ID})
			if err != nil {
				webutil.ServerError(w, err)
				return
			}
			for _, starting := range startingSchedules(header.Kind) {
				if starting.Kind == s.Kind {
					s.Personal = starting.Personal
				}
			}
		}
	}

	if err := normalizeSchedule(&s); err != nil {
		webutil.BadRequest(w, err.Error())
		return
	}
	tx, err := h.DB.Begin(r.Context())
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	defer tx.Rollback(r.Context())
	q := query.New(tx)
	header, err := q.GetVehicleHistoryHeader(r.Context(), query.GetVehicleHistoryHeaderParams{ID: vehicleID, UserID: user.ID})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	before, err := loadMaintenanceSchedules(r.Context(), q, vehicleID, user.ID, header.Kind)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	var old maintenanceSchedule
	for _, setting := range before {
		if setting.Kind == s.Kind {
			old = setting
		}
	}
	count, err := q.SaveMaintenanceSchedule(r.Context(), query.SaveMaintenanceScheduleParams{VehicleID: vehicleID, UserID: user.ID, Kind: s.Kind, ActiveSource: s.ActiveSource, PersonalKm: s.Personal.Kilometers, PersonalDuration: s.Personal.Duration, PersonalUnit: s.Personal.Unit, ManualKm: s.Manual.Kilometers, ManualDuration: s.Manual.Duration, ManualUnit: s.Manual.Unit, ManualReference: s.ManualReference})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	if count == 0 {
		http.NotFound(w, r)
		return
	}
	if s.Kind == "tire_pressure" && old.effective() != s.effective() {
		if err := q.DismissPressureScheduleAlerts(r.Context(), query.DismissPressureScheduleAlertsParams{UserID: user.ID, VehicleID: vehicleID}); err != nil {
			webutil.ServerError(w, err)
			return
		}
		if err := q.ResetLatestPressureReminder(r.Context(), query.ResetLatestPressureReminderParams{VehicleID: vehicleID, UserID: user.ID}); err != nil {
			webutil.ServerError(w, err)
			return
		}
	}
	if err := tx.Commit(r.Context()); err != nil {
		webutil.ServerError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, map[string]bool{"saved": true})
}
