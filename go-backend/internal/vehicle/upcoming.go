package vehicle

import (
	"math"
	"slices"
	"time"
)

type upcomingMaintenance struct {
	DistanceUnavailable    bool       `json:"distance_unavailable"`
	Source                 string     `json:"source"`
	Kind                   string     `json:"kind"`
	Status                 string     `json:"status"`
	DueDate                *string    `json:"due_date"`
	RemainingDays          *int       `json:"remaining_days"`
	RemainingKM            *float64   `json:"remaining_km"`
	Progress               *float64   `json:"progress"`
	LastAt                 *time.Time `json:"last_at"`
	LastRecordedOdometerKM *float64   `json:"last_recorded_odometer_km"`
	IntervalKM             int        `json:"interval_km"`
	IntervalMonths         int        `json:"interval_months"`
	IntervalDays           int        `json:"interval_days"`
}

func calculateUpcoming(kind string, fuels []string, air []airFillHistory, fills []fuelFillHistory, records []maintenanceRecord, now time.Time, schedules ...maintenanceSchedule) []upcomingMaintenance {
	location := time.FixedZone("India", 19800)
	now = now.In(location)
	settings := map[string]maintenanceSchedule{}
	for _, setting := range startingSchedules(kind) {
		settings[setting.Kind] = setting
	}
	for _, setting := range schedules {
		settings[setting.Kind] = setting
	}
	var mileage *float64
	var mileageAt time.Time
	for _, f := range fills {
		if !f.FilledAt.After(now) && f.FilledAt.After(mileageAt) {
			mileage, mileageAt = &f.OdometerKM, f.FilledAt
		}
	}
	for _, r := range records {
		if r.OdometerKM != nil && !r.OccurredAt.After(now) && (r.OccurredAt.After(mileageAt) || (r.OccurredAt.Equal(mileageAt) && (mileage == nil || *r.OdometerKM > *mileage))) {
			mileage, mileageAt = r.OdometerKM, r.OccurredAt
		}
	}
	for _, a := range air {
		if a.OdometerKM != nil && !a.FilledAt.After(now) && (a.FilledAt.After(mileageAt) || (a.FilledAt.Equal(mileageAt) && (mileage == nil || *a.OdometerKM > *mileage))) {
			mileage, mileageAt = a.OdometerKM, a.FilledAt
		}
	}
	result := []upcomingMaintenance{}
	find := func(component, action string) *maintenanceRecord {
		var latest *maintenanceRecord
		for i := range records {
			r := &records[i]
			if r.OccurredAt.After(now) {
				continue
			}
			for _, a := range r.Actions {
				if a.Component == component && a.Action == action && (latest == nil || r.OccurredAt.After(latest.OccurredAt) || (r.OccurredAt.Equal(latest.OccurredAt) && r.ID > latest.ID)) {
					latest = r
				}
			}
		}
		return latest
	}
	build := func(key string, at *time.Time, baselineKM *float64) upcomingMaintenance {
		setting := settings[key]
		interval := setting.effective()
		km, months, days := int(interval.Kilometers), 0, 0
		if interval.Unit == "months" {
			months = int(interval.Duration)
		} else {
			days = int(interval.Duration)
		}
		item := upcomingMaintenance{Kind: key, Source: setting.ActiveSource, Status: "no_history", IntervalKM: km, IntervalMonths: months, IntervalDays: days, LastAt: at, LastRecordedOdometerKM: mileage}
		if at == nil {
			return item
		}
		start := dayIn(*at, location)
		due := start.AddDate(0, 0, days)
		if months > 0 {
			// Clamp month ends rather than letting AddDate roll into the following month.
			first := time.Date(start.Year(), start.Month()+time.Month(months), 1, 0, 0, 0, 0, location)
			lastDay := first.AddDate(0, 1, -1).Day()
			due = first.AddDate(0, 0, min(start.Day(), lastDay)-1)
		}
		progress := 0.0
		item.Status = "upcoming"
		if months > 0 || days > 0 {
			date := due.Format("2006-01-02")
			remaining := int(due.Sub(dayIn(now, location)).Hours() / 24)
			item.DueDate, item.RemainingDays = &date, &remaining
			progress = now.Sub(start).Seconds() / due.Sub(start).Seconds()
			if remaining == 0 {
				item.Status = "due"
			} else if remaining < 0 {
				item.Status = "overdue"
			}
		}
		if km > 0 && mileage != nil && baselineKM != nil && !mileageAt.Before(*at) && *mileage >= *baselineKM {
			distance := *baselineKM + float64(km) - *mileage
			item.RemainingKM = &distance
			progress = math.Max(progress, (*mileage-*baselineKM)/float64(km))
			if distance < 0 {
				item.Status = "overdue"
			} else if distance == 0 && item.Status != "overdue" {
				item.Status = "due"
			}
		}
		item.DistanceUnavailable = km > 0 && item.RemainingKM == nil
		if item.DueDate == nil && item.RemainingKM == nil {
			item.Status = "needs_odometer"
			return item
		}
		progress = math.Max(0, math.Min(1, progress))
		item.Progress = &progress
		return item
	}
	if (settings["engine_oil"].effective().Kilometers > 0 || settings["engine_oil"].effective().Duration > 0) && slices.ContainsFunc(fuels, func(f string) bool { return f != "electric" }) {
		var at *time.Time
		var odometer *float64
		if r := find("engine_oil", "replaced"); r != nil {
			at, odometer = &r.OccurredAt, r.OdometerKM
		}
		result = append(result, build("engine_oil", at, odometer))
	}
	var lastAir *time.Time
	var airOdometer *float64
	for i := range air {
		if !air[i].FilledAt.After(now) && (lastAir == nil || air[i].FilledAt.After(*lastAir)) {
			lastAir = &air[i].FilledAt
			airOdometer = air[i].OdometerKM
		}
	}
	result = append(result, build("tire_pressure", lastAir, airOdometer))
	if settings["general_service"].effective().Kilometers > 0 || settings["general_service"].effective().Duration > 0 {
		var at *time.Time
		var odometer *float64
		if r := find("general_service", "completed"); r != nil {
			at, odometer = &r.OccurredAt, r.OdometerKM
		}
		result = append(result, build("general_service", at, odometer))
	}
	return result
}

func dayIn(t time.Time, location *time.Location) time.Time {
	t = t.In(location)
	return time.Date(t.Year(), t.Month(), t.Day(), 0, 0, 0, 0, location)
}
