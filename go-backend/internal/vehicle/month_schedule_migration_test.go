package vehicle

import (
	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"os"
	"testing"
)

func TestMonthScheduleMigration(t *testing.T) {
	db, stop := testhelper.StartPostgres(t)
	defer stop()
	migrate := func(direction string) {
		t.Helper()
		sql, err := os.ReadFile("../../migrations/00035_reminder_month_intervals." + direction + ".sql")
		if err != nil {
			t.Fatal(err)
		}
		if _, err := db.Exec(string(sql)); err != nil {
			t.Fatal(err)
		}
	}
	migrate("down")
	var changed, unchanged int64
	for i, target := range []*int64{&changed, &unchanged} {
		if err := db.QueryRow(`INSERT INTO vehicles(name,user_id,kind,fuel_types) VALUES($1,1,'motorcycle',ARRAY['petrol']) RETURNING id`, []string{"Changed", "Inactive only"}[i]).Scan(target); err != nil {
			t.Fatal(err)
		}
	}
	if _, err := db.Exec(`INSERT INTO vehicle_maintenance_schedules(vehicle_id,user_id,kind,active_source,personal_km,personal_duration,personal_unit,manual_km,manual_duration,manual_unit,manual_reference)
 VALUES($1,1,'tire_pressure','personal',500,14,'days',900,60,'days','Page 42'),
 ($2,1,'tire_pressure','manual',800,30,'days',1500,6,'months','Keep manual'),
 ($1,1,'engine_oil','personal',3000,45,'days',5000,36500,'days','Oil manual')`, changed, unchanged); err != nil {
		t.Fatal(err)
	}
	seedFill := func(vehicle int64, when string) (int64, int64) {
		t.Helper()
		var alert, fill int64
		if err := db.QueryRow(`INSERT INTO notifications(user_id,source,title) VALUES(1,'Garage','Pressure due') RETURNING id`).Scan(&alert); err != nil {
			t.Fatal(err)
		}
		if err := db.QueryRow(`INSERT INTO vehicle_air_fills(vehicle_id,user_id,filled_at,reminder_notification_id) VALUES($1,1,$2,$3) RETURNING id`, vehicle, when, alert).Scan(&fill); err != nil {
			t.Fatal(err)
		}
		return fill, alert
	}
	older, oldAlert := seedFill(changed, "2026-09-01T12:00:00Z")
	latest, latestAlert := seedFill(changed, "2026-10-01T12:00:00Z")
	untouched, untouchedAlert := seedFill(unchanged, "2026-10-01T12:00:00Z")
	migrate("up")
	var source, personalUnit, manualUnit, reference string
	var km, months, manualMonths int
	if err := db.QueryRow(`SELECT active_source,personal_km,personal_duration,personal_unit,manual_duration,manual_unit,manual_reference FROM vehicle_maintenance_schedules WHERE vehicle_id=$1 AND kind='tire_pressure'`, changed).Scan(&source, &km, &months, &personalUnit, &manualMonths, &manualUnit, &reference); err != nil {
		t.Fatal(err)
	}
	if source != "personal" || km != 500 || months != 1 || personalUnit != "months" || manualMonths != 2 || manualUnit != "months" || reference != "Page 42" {
		t.Fatalf("unexpected conversion: %s %d %d %s %d %s %s", source, km, months, personalUnit, manualMonths, manualUnit, reference)
	}
	if err := db.QueryRow(`SELECT personal_duration,manual_duration FROM vehicle_maintenance_schedules WHERE vehicle_id=$1 AND kind='engine_oil'`, changed).Scan(&months, &manualMonths); err != nil || months != 2 || manualMonths != 1200 {
		t.Fatalf("long conversion: %d %d %v", months, manualMonths, err)
	}
	for _, tc := range []struct {
		fill, alert        int64
		cleared, dismissed bool
	}{{older, oldAlert, false, true}, {latest, latestAlert, true, true}, {untouched, untouchedAlert, false, false}} {
		var cleared, dismissed bool
		if err := db.QueryRow(`SELECT reminder_notification_id IS NULL FROM vehicle_air_fills WHERE id=$1`, tc.fill).Scan(&cleared); err != nil {
			t.Fatal(err)
		}
		if err := db.QueryRow(`SELECT dismissed_at IS NOT NULL FROM notifications WHERE id=$1`, tc.alert).Scan(&dismissed); err != nil {
			t.Fatal(err)
		}
		if cleared != tc.cleared || dismissed != tc.dismissed {
			t.Fatalf("wrong alert reset for fill %d: %v %v", tc.fill, cleared, dismissed)
		}
	}
	if _, err := db.Exec(`UPDATE vehicle_maintenance_schedules SET personal_unit='days' WHERE vehicle_id=$1`, changed); err == nil {
		t.Fatal("day-based rules still accepted")
	}
	migrate("down")
	if err := db.QueryRow(`SELECT personal_duration FROM vehicle_maintenance_schedules WHERE vehicle_id=$1 AND kind='tire_pressure'`, changed).Scan(&months); err != nil || months != 1 {
		t.Fatal("rollback changed converted values", err)
	}
}

func TestLegacyDayInputUsesMonths(t *testing.T) {
	for _, tc := range []struct{ days, months int32 }{{0, 0}, {14, 1}, {30, 1}, {45, 2}, {365, 12}, {36500, 1200}} {
		s := maintenanceSchedule{Kind: "engine_oil", ActiveSource: "personal", Personal: scheduleInterval{3000, tc.days, "days"}}
		if err := normalizeSchedule(&s); err != nil || s.Personal.Duration != tc.months || s.Personal.Unit != "months" {
			t.Fatalf("days=%d got=%+v err=%v", tc.days, s.Personal, err)
		}
	}
}
