package vehicle

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strconv"
	"testing"
	"time"

	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/DarkAbhi/life-backend/internal/notification"
	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"github.com/go-chi/chi/v5"
)

func TestConfiguredUpcoming(t *testing.T) {
	now := time.Date(2026, 11, 2, 12, 0, 0, 0, time.UTC)
	at := time.Date(2026, 9, 7, 9, 0, 0, 0, time.UTC)
	records := []maintenanceRecord{{ID: 1, OccurredAt: at, OdometerKM: floatPtr(1000), Actions: []maintenanceAction{{Component: "engine_oil", Action: "replaced"}}}}
	s := maintenanceSchedule{Kind: "engine_oil", ActiveSource: "manual", Personal: scheduleInterval{1000, 1, "months"}, Manual: scheduleInterval{5000, 6, "months"}}
	result := calculateUpcoming("motorcycle", []string{"petrol"}, nil, nil, records, now, s)[0]
	if result.Source != "manual" || *result.DueDate != "2027-03-07" || *result.RemainingKM != 5000 {
		t.Fatalf("inactive personal leaked into manual: %+v", result)
	}
	s.ActiveSource = "personal"
	result = calculateUpcoming("motorcycle", []string{"petrol"}, nil, nil, records, now, s)[0]
	if result.Status != "overdue" || *result.DueDate != "2026-10-07" {
		t.Fatalf("personal source: %+v", result)
	}
	s.Personal = scheduleInterval{2000, 0, "months"}
	result = calculateUpcoming("motorcycle", []string{"petrol"}, nil, nil, records, now, s)[0]
	if result.DueDate != nil || result.RemainingDays != nil || *result.RemainingKM != 2000 {
		t.Fatalf("km-only invented date: %+v", result)
	}
	records[0].OdometerKM = nil
	result = calculateUpcoming("motorcycle", []string{"petrol"}, nil, nil, records, now, s)[0]
	if result.Status != "needs_odometer" || result.Progress != nil {
		t.Fatalf("missing mileage: %+v", result)
	}
	s.Personal = scheduleInterval{0, 10, "days"}
	result = calculateUpcoming("motorcycle", []string{"petrol"}, nil, nil, records, now, s)[0]
	if result.RemainingKM != nil || *result.DueDate != "2026-09-17" {
		t.Fatalf("time-only: %+v", result)
	}
}

func TestScheduleValidation(t *testing.T) {
	for _, tc := range []struct {
		name string
		s    maintenanceSchedule
		bad  bool
	}{
		{"empty active", maintenanceSchedule{Kind: "engine_oil", ActiveSource: "personal"}, true},
		{"negative", maintenanceSchedule{Kind: "engine_oil", ActiveSource: "manual", Manual: scheduleInterval{-1, 6, "months"}}, true},
		{"invalid unit", maintenanceSchedule{Kind: "engine_oil", ActiveSource: "manual", Manual: scheduleInterval{1000, 1, "years"}}, true},
		{"excess months", maintenanceSchedule{Kind: "engine_oil", ActiveSource: "manual", Manual: scheduleInterval{0, 1201, "months"}}, true},
		{"defaults and empty saved sets", maintenanceSchedule{Kind: "engine_oil", ActiveSource: "default"}, false},
		{"pressure distance", maintenanceSchedule{Kind: "tire_pressure", ActiveSource: "personal", Personal: scheduleInterval{500, 0, "days"}}, false},
	} {
		t.Run(tc.name, func(t *testing.T) {
			if err := normalizeSchedule(&tc.s); (err != nil) != tc.bad {
				t.Fatalf("err=%v", err)
			}
		})
	}
}

func TestMaintenanceSchedules(t *testing.T) {
	db, dsn, stop := testhelper.StartPostgresWithDSN(t)
	defer stop()
	pool := newTestPool(t, dsn)
	defer pool.Close()
	var vehicle, other int64
	if err := db.QueryRow(`INSERT INTO vehicles(name,user_id,kind,fuel_types) VALUES('Schedule car',1,'car',ARRAY['petrol']) RETURNING id`).Scan(&vehicle); err != nil {
		t.Fatal(err)
	}
	if err := db.QueryRow(`INSERT INTO users(username,password_hash) VALUES('schedule-other','x') RETURNING id`).Scan(&other); err != nil {
		t.Fatal(err)
	}
	h := NewHandler(pool, NewService(NewRepository(pool)), func(r *http.Request) (auth.SessionUser, error) {
		if r.Header.Get("X-Other") == "true" {
			return auth.SessionUser{ID: other}, nil
		}
		return auth.SessionUser{ID: 1}, nil
	})
	router := chi.NewRouter()
	h.RegisterRoutes(router)
	path := "/vehicles/" + strconv.FormatInt(vehicle, 10)
	request := func(method, suffix string, payload any, otherUser bool, want int) *httptest.ResponseRecorder {
		t.Helper()
		var body []byte
		if payload != nil {
			var err error
			body, err = json.Marshal(payload)
			if err != nil {
				t.Fatal(err)
			}
		}
		req := httptest.NewRequest(method, path+suffix, bytes.NewReader(body))
		if otherUser {
			req.Header.Set("X-Other", "true")
		}
		rec := httptest.NewRecorder()
		router.ServeHTTP(rec, req)
		if rec.Code != want {
			t.Fatalf("%s %s: %d %s", method, suffix, rec.Code, rec.Body.String())
		}
		return rec
	}
	setting := maintenanceSchedule{Kind: "engine_oil", ActiveSource: "manual", Personal: scheduleInterval{3000, 4, "months"}, Manual: scheduleInterval{5000, 6, "months"}, ManualReference: "Manual p42"}
	request("PUT", "/maintenance-schedules/engine_oil", setting, false, 200)
	request("PUT", "/maintenance-schedules/engine_oil", setting, true, 404)
	var history struct {
		Schedules []maintenanceSchedule `json:"maintenance_schedules"`
	}
	if err := json.Unmarshal(request("GET", "/history", nil, false, 200).Body.Bytes(), &history); err != nil {
		t.Fatal(err)
	}
	if len(history.Schedules) != 3 || history.Schedules[0].ActiveSource != "manual" || history.Schedules[0].Personal.Kilometers != 3000 || history.Schedules[0].ManualReference != "Manual p42" {
		t.Fatalf("saved settings: %+v", history)
	}
	setting.ActiveSource = "personal"
	request("PUT", "/maintenance-schedules/engine_oil", setting, false, 200)
	setting.Personal.Duration = -1
	request("PUT", "/maintenance-schedules/engine_oil", setting, false, 400)
	var source string
	if err := db.QueryRow(`SELECT active_source FROM vehicle_maintenance_schedules WHERE vehicle_id=$1 AND kind='engine_oil'`, vehicle).Scan(&source); err != nil || source != "personal" {
		t.Fatalf("invalid update modified schedule: %s %v", source, err)
	}
	if _, err := db.Exec(`INSERT INTO vehicle_maintenance_schedules(vehicle_id,user_id,kind,active_source) VALUES($1,$2,'tire_pressure','default')`, vehicle, other); err == nil {
		t.Fatal("cross-owner schedule accepted")
	}
	// Start a pressure cycle with a known odometer, then backdate it twenty days.
	request("POST", "/air-fills", map[string]any{"odometer_km": 1000}, false, 201)
	if _, err := db.Exec(`UPDATE vehicle_air_fills SET filled_at=NOW()-INTERVAL '20 days' WHERE vehicle_id=$1`, vehicle); err != nil {
		t.Fatal(err)
	}
	pressure := maintenanceSchedule{Kind: "tire_pressure", ActiveSource: "personal", Personal: scheduleInterval{0, 7, "days"}, Manual: scheduleInterval{0, 45, "days"}}
	request("PUT", "/maintenance-schedules/tire_pressure", pressure, false, 200)
	reminders := NewReminderService(pool, notification.NewService(notification.NewRepository(pool)))
	if err := reminders.CreateDue(context.Background()); err != nil {
		t.Fatal(err)
	}
	var active int
	countAlerts := func() int {
		t.Helper()
		if err := db.QueryRow(`SELECT COUNT(*) FROM notifications WHERE user_id=1 AND source='Garage' AND dismissed_at IS NULL`).Scan(&active); err != nil {
			t.Fatal(err)
		}
		return active
	}
	if countAlerts() != 1 {
		t.Fatal("selected seven-day pressure rule did not notify")
	}
	pressure.ActiveSource = "manual"
	request("PUT", "/maintenance-schedules/tire_pressure", pressure, false, 200)
	if countAlerts() != 0 {
		t.Fatal("stale pressure alert not dismissed")
	}
	if err := reminders.CreateDue(context.Background()); err != nil {
		t.Fatal(err)
	}
	if countAlerts() != 0 {
		t.Fatal("inactive personal rule still sent a reminder")
	}
	pressure.ActiveSource = "personal"
	pressure.Personal = scheduleInterval{500, 0, "days"}
	request("PUT", "/maintenance-schedules/tire_pressure", pressure, false, 200)
	if _, err := db.Exec(`INSERT INTO vehicle_fuel_fillups(vehicle_id,user_id,odometer_km,filled_at) VALUES($1,1,1600,NOW())`, vehicle); err != nil {
		t.Fatal(err)
	}
	if err := reminders.CreateDue(context.Background()); err != nil {
		t.Fatal(err)
	}
	if countAlerts() != 1 {
		t.Fatal("distance-only pressure rule did not notify")
	}
	request("DELETE", "", nil, false, 204)
	var left int
	if err := db.QueryRow(`SELECT COUNT(*) FROM vehicle_maintenance_schedules WHERE vehicle_id=$1`, vehicle).Scan(&left); err != nil || left != 0 {
		t.Fatalf("cascade: %d %v", left, err)
	}
}
