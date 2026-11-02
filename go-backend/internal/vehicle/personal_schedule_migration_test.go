package vehicle

import (
	"context"
	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"github.com/DarkAbhi/life-backend/internal/vehicle/query"
	"os"
	"testing"
)

func TestPersonalScheduleMigration(t *testing.T) {
	db, dsn, stop := testhelper.StartPostgresWithDSN(t)
	defer stop()
	migrate := func(direction string) {
		t.Helper()
		sql, err := os.ReadFile("../../migrations/00034_personal_reminder_starting_values." + direction + ".sql")
		if err != nil {
			t.Fatal(err)
		}
		if _, err := db.Exec(string(sql)); err != nil {
			t.Fatal(err)
		}
	}
	changeMonths := func(direction string) {
		t.Helper()
		sql, err := os.ReadFile("../../migrations/00035_reminder_month_intervals." + direction + ".sql")
		if err != nil {
			t.Fatal(err)
		}
		if _, err := db.Exec(string(sql)); err != nil {
			t.Fatal(err)
		}
	}
	changeMonths("down")
	migrate("down")
	ids := map[string]int64{}
	for _, v := range []struct{ name, kind, fuel string }{{"Bike", "motorcycle", "petrol"}, {"Car", "car", "petrol"}, {"Van", "van", "diesel"}, {"Electric", "car", "electric"}} {
		var id int64
		if err := db.QueryRow(`INSERT INTO vehicles(name,user_id,kind,fuel_types) VALUES($1,1,$2,ARRAY[$3]::text[]) RETURNING id`, v.name, v.kind, v.fuel).Scan(&id); err != nil {
			t.Fatal(err)
		}
		ids[v.name] = id
	}
	for _, row := range []struct {
		name, task, source string
		km, duration       int
		manualKM           int
	}{
		{"Bike", "engine_oil", "default", 0, 0, 7000},
		{"Car", "general_service", "default", 4321, 2, 8000},
		{"Van", "engine_oil", "default", 0, 0, 0},
		{"Electric", "engine_oil", "default", 0, 0, 0},
		{"Bike", "general_service", "manual", 0, 0, 6000},
	} {
		if _, err := db.Exec(`INSERT INTO vehicle_maintenance_schedules(vehicle_id,user_id,kind,active_source,personal_km,personal_duration,manual_km,manual_duration,manual_reference) VALUES($1,1,$2,$3,$4,$5,$6,6,'Manual p42')`, ids[row.name], row.task, row.source, row.km, row.duration, row.manualKM); err != nil {
			t.Fatal(err)
		}
	}
	migrate("up")
	for _, row := range []struct {
		name, task, source     string
		km, duration, manualKM int
	}{
		{"Bike", "engine_oil", "personal", 3000, 4, 7000},
		{"Car", "general_service", "personal", 4321, 2, 8000},
		{"Van", "engine_oil", "personal", 0, 0, 0},
		{"Electric", "engine_oil", "personal", 0, 0, 0},
		{"Bike", "general_service", "manual", 5000, 6, 6000},
	} {
		var source, reference string
		var km, duration, manualKM int
		if err := db.QueryRow(`SELECT active_source,personal_km,personal_duration,manual_km,manual_reference FROM vehicle_maintenance_schedules WHERE vehicle_id=$1 AND kind=$2`, ids[row.name], row.task).Scan(&source, &km, &duration, &manualKM, &reference); err != nil {
			t.Fatal(err)
		}
		if source != row.source || km != row.km || duration != row.duration || manualKM != row.manualKM || reference != "Manual p42" {
			t.Fatalf("%s/%s changed: %s %d %d %d %s", row.name, row.task, source, km, duration, manualKM, reference)
		}
	}
	var count int
	if err := db.QueryRow(`SELECT COUNT(*) FROM vehicle_maintenance_schedules WHERE vehicle_id=$1`, ids["Bike"]).Scan(&count); err != nil || count != 3 {
		t.Fatalf("missing tasks: %d %v", count, err)
	}
	migrate("down")
	migrate("up")
	changeMonths("up")
	pool := newTestPool(t, dsn)
	defer pool.Close()
	vehicle, err := NewRepository(pool).Create(context.Background(), query.CreateVehicleParams{Name: "New bike", UserID: 1, Kind: "motorcycle", FuelTypes: []string{"petrol"}, IsActive: true})
	if err != nil {
		t.Fatal(err)
	}
	var km int
	if err := db.QueryRow(`SELECT personal_km FROM vehicle_maintenance_schedules WHERE vehicle_id=$1 AND kind='engine_oil' AND active_source='personal'`, vehicle.ID).Scan(&km); err != nil || km != 3000 {
		t.Fatalf("new vehicle not prefilled: %d %v", km, err)
	}
}
