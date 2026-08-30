package vehicle

import (
	"bytes"
	"context"
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strconv"
	"testing"
	"time"

	"github.com/go-chi/chi/v5"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
)

func loginUser(t *testing.T, db *sql.DB) *http.Cookie {
	t.Helper()
	token := "vehicletesttoken"
	hash := sha256.Sum256([]byte(token))
	hashStr := hex.EncodeToString(hash[:])
	expiresAt := time.Now().Add(24 * time.Hour)
	_, err := db.Exec(`
		INSERT INTO user_sessions (user_id, token_hash, expires_at)
		VALUES (1, $1, $2)
	`, hashStr, expiresAt)
	if err != nil {
		t.Fatalf("failed to insert session: %v", err)
	}
	return &http.Cookie{
		Name:  "life_session",
		Value: token,
	}
}

func TestVehiclesCRUD(t *testing.T) {
	db, shutdown := testhelper.StartPostgres(t)
	defer shutdown()

	h := NewHandler(db)

	// 1. Create vehicle - missing name
	{
		rec := httptest.NewRecorder()
		body, _ := json.Marshal(vehiclePayload{Name: nil})
		req := httptest.NewRequest(http.MethodPost, "/vehicles", bytes.NewReader(body))
		h.CreateVehicle(rec, req)
		if rec.Code != http.StatusBadRequest {
			t.Errorf("expected 400 Bad Request, got %d", rec.Code)
		}
	}

	// 2. Create vehicle - success
	var vehicleID int64
	{
		rec := httptest.NewRecorder()
		name := "Tesla Model 3"
		body, _ := json.Marshal(vehiclePayload{Name: &name})
		req := httptest.NewRequest(http.MethodPost, "/vehicles", bytes.NewReader(body))
		h.CreateVehicle(rec, req)

		if rec.Code != http.StatusCreated {
			t.Errorf("expected 201 Created, got %d", rec.Code)
		}

		var out vehicleDTO
		_ = json.NewDecoder(rec.Body).Decode(&out)
		if out.Name != "Tesla Model 3" || !out.IsActive {
			t.Errorf("unexpected vehicle response: %+v", out)
		}
		vehicleID = out.ID
	}

	// 3. List vehicles
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodGet, "/vehicles", nil)
		h.ListVehicles(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected 200, got %d", rec.Code)
		}

		var out []struct {
			ID   int64  `json:"id"`
			Name string `json:"name"`
		}
		_ = json.NewDecoder(rec.Body).Decode(&out)
		if len(out) != 1 || out[0].Name != "Tesla Model 3" {
			t.Errorf("expected 1 vehicle named Tesla Model 3, got %+v", out)
		}
	}

	// 4. Get vehicle by ID
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodGet, "/vehicles/{id}", nil)
		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(vehicleID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.GetVehicle(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected 200, got %d", rec.Code)
		}

		var out vehicleDTO
		_ = json.NewDecoder(rec.Body).Decode(&out)
		if out.ID != vehicleID || out.Name != "Tesla Model 3" {
			t.Errorf("unexpected vehicle details: %+v", out)
		}
	}

	// 5. Update vehicle
	{
		rec := httptest.NewRecorder()
		name := "Tesla Model S"
		active := false
		body, _ := json.Marshal(vehiclePayload{Name: &name, IsActive: &active})
		req := httptest.NewRequest(http.MethodPut, "/vehicles/{id}", bytes.NewReader(body))
		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(vehicleID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.UpdateVehicle(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected 200, got %d", rec.Code)
		}

		var out vehicleDTO
		_ = json.NewDecoder(rec.Body).Decode(&out)
		if out.Name != "Tesla Model S" || out.IsActive {
			t.Errorf("unexpected updated response: %+v", out)
		}
	}

	// 6. Delete vehicle
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodDelete, "/vehicles/{id}", nil)
		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(vehicleID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.DeleteVehicle(rec, req)

		if rec.Code != http.StatusNoContent {
			t.Errorf("expected 204, got %d", rec.Code)
		}

		// Verify deletion
		var count int
		_ = db.QueryRow(`SELECT COUNT(*) FROM vehicles WHERE id = $1`, vehicleID).Scan(&count)
		if count != 0 {
			t.Errorf("expected vehicle to be deleted, got count %d", count)
		}
	}
}

func TestFuelFillupsAndEconomy(t *testing.T) {
	db, shutdown := testhelper.StartPostgres(t)
	defer shutdown()

	h := NewHandler(db)
	cookie := loginUser(t, db)

	// Seed vehicle
	var vehicleID int64
	err := db.QueryRow(`INSERT INTO vehicles (name) VALUES ('Tesla') RETURNING id`).Scan(&vehicleID)
	if err != nil {
		t.Fatalf("failed to seed: %v", err)
	}

	// 1. Create first fuel entry - full tank
	{
		rec := httptest.NewRecorder()
		qty := 30.0
		cost := 120.0
		body, _ := json.Marshal(fuelFillupInput{
			OdometerKM: 1000.0,
			Items: []fuelItemInput{
				{FuelType: "petrol", FillType: "full", Quantity: &qty, TotalCost: &cost},
			},
		})
		req := httptest.NewRequest(http.MethodPost, "/vehicles/{id}/fuel-fillups", bytes.NewReader(body))
		req.AddCookie(cookie)
		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(vehicleID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.CreateFuelFillup(rec, req)

		if rec.Code != http.StatusCreated {
			t.Errorf("expected 201, got %d", rec.Code)
		}

		var out map[string]any
		_ = json.NewDecoder(rec.Body).Decode(&out)
		economies := out["economy_km_per_litre"].(map[string]any)
		if economies["petrol"] != nil {
			t.Errorf("economy should be nil for first fillup, got %v", economies["petrol"])
		}
	}

	// 2. Create second fuel entry - full tank (can compute economy)
	{
		rec := httptest.NewRecorder()
		qty := 20.0
		cost := 80.0
		body, _ := json.Marshal(fuelFillupInput{
			OdometerKM: 1300.0, // drove 300 km
			Items: []fuelItemInput{
				{FuelType: "petrol", FillType: "full", Quantity: &qty, TotalCost: &cost},
			},
		})
		req := httptest.NewRequest(http.MethodPost, "/vehicles/{id}/fuel-fillups", bytes.NewReader(body))
		req.AddCookie(cookie)
		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(vehicleID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.CreateFuelFillup(rec, req)

		if rec.Code != http.StatusCreated {
			t.Errorf("expected 201, got %d", rec.Code)
		}

		var out map[string]any
		_ = json.NewDecoder(rec.Body).Decode(&out)
		economies := out["economy_km_per_litre"].(map[string]any)
		if economies["petrol"].(float64) != 15.0 { // 300 km / 20 L = 15 km/L
			t.Errorf("expected economy to be 15.0, got %v", economies["petrol"])
		}
	}

	// 3. Vehicle history exposes the average mileage from all reliable intervals.
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodGet, "/vehicles/{id}/history", nil)
		req.AddCookie(cookie)
		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(vehicleID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.VehicleHistory(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected 200, got %d", rec.Code)
		}
		var out map[string]any
		_ = json.NewDecoder(rec.Body).Decode(&out)
		averages := out["average_mileage_km_per_litre"].(map[string]any)
		if averages["petrol"].(float64) != 15.0 {
			t.Errorf("expected average mileage to be 15.0, got %v", averages["petrol"])
		}
	}
}

func TestAirFillsAndReminders(t *testing.T) {
	db, shutdown := testhelper.StartPostgres(t)
	defer shutdown()

	h := NewHandler(db)
	cookie := loginUser(t, db)

	// Seed vehicle
	var vehicleID int64
	err := db.QueryRow(`INSERT INTO vehicles (name) VALUES ('Honda City') RETURNING id`).Scan(&vehicleID)
	if err != nil {
		t.Fatalf("failed to seed: %v", err)
	}

	// 1. Create air fill
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodPost, "/vehicles/{id}/air-fills", nil)
		req.AddCookie(cookie)
		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(vehicleID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.CreateVehicleAirFill(rec, req)

		if rec.Code != http.StatusCreated {
			t.Errorf("expected 201, got %d", rec.Code)
		}

		var out vehicleAirFillDTO
		_ = json.NewDecoder(rec.Body).Decode(&out)
		if out.VehicleID != vehicleID {
			t.Errorf("expected vehicle ID %d, got %d", vehicleID, out.VehicleID)
		}
	}

	// 2. List latest air fills
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodGet, "/vehicle-air-fills/latest", nil)
		req.AddCookie(cookie)

		h.ListLatestVehicleAirFills(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected 200, got %d", rec.Code)
		}

		var out []vehicleAirFillDTO
		_ = json.NewDecoder(rec.Body).Decode(&out)
		if len(out) != 1 || out[0].VehicleID != vehicleID {
			t.Errorf("unexpected latest air fills: %+v", out)
		}
	}

	// 3. Test background air-fill reminder job
	// Update filled_at to be 31 days ago
	_, err = db.Exec(`UPDATE vehicle_air_fills SET filled_at = NOW() - INTERVAL '31 days'`)
	if err != nil {
		t.Fatalf("failed to update filled_at: %v", err)
	}

	createDueAirFillReminders(db)

	// Verify notification generated
	var notifCount int
	err = db.QueryRow(`
		SELECT COUNT(*) FROM notifications
		WHERE user_id = 1 AND source = 'Garage' AND title = 'Time to check Honda City''s air'
	`).Scan(&notifCount)
	if err != nil {
		t.Fatalf("failed to count: %v", err)
	}
	if notifCount != 1 {
		t.Errorf("expected 1 reminder notification, got %d", notifCount)
	}
}

func TestVehicleHistoryAndDeleteLogs(t *testing.T) {
	db, shutdown := testhelper.StartPostgres(t)
	defer shutdown()

	h := NewHandler(db)
	cookie := loginUser(t, db)

	// Seed vehicle
	var vehicleID int64
	err := db.QueryRow(`INSERT INTO vehicles (name) VALUES ('Honda City') RETURNING id`).Scan(&vehicleID)
	if err != nil {
		t.Fatalf("failed to seed: %v", err)
	}

	// Seed one air fill and one fuel fillup
	var airFillID, fuelFillupID int64
	err = db.QueryRow(`INSERT INTO vehicle_air_fills (vehicle_id, user_id) VALUES ($1, 1) RETURNING id`, vehicleID).Scan(&airFillID)
	if err != nil {
		t.Fatalf("failed to seed: %v", err)
	}
	err = db.QueryRow(`INSERT INTO vehicle_fuel_fillups (vehicle_id, user_id, odometer_km) VALUES ($1, 1, 1200) RETURNING id`, vehicleID).Scan(&fuelFillupID)
	if err != nil {
		t.Fatalf("failed to seed: %v", err)
	}
	_, err = db.Exec(`INSERT INTO vehicle_fuel_items (fillup_id, fuel_type, fill_type, quantity, unit_price, total_cost) VALUES ($1, 'petrol', 'full', 10, 10, 100)`, fuelFillupID)
	if err != nil {
		t.Fatalf("failed to seed: %v", err)
	}

	// 1. Get vehicle history
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodGet, "/vehicles/{id}/history", nil)
		req.AddCookie(cookie)
		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(vehicleID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.VehicleHistory(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected 200, got %d", rec.Code)
		}

		var out map[string]any
		_ = json.NewDecoder(rec.Body).Decode(&out)
		if out["vehicle_name"].(string) != "Honda City" {
			t.Errorf("expected Honda City, got %v", out["vehicle_name"])
		}
		if len(out["air_fills"].([]any)) != 1 {
			t.Errorf("expected 1 air fill, got %v", out["air_fills"])
		}
		if len(out["fuel_fillups"].([]any)) != 1 {
			t.Errorf("expected 1 fuel fillup, got %v", out["fuel_fillups"])
		}
		if len(out["average_mileage_km_per_litre"].(map[string]any)) != 0 {
			t.Errorf("expected no mileage with one fill-up, got %v", out["average_mileage_km_per_litre"])
		}
	}

	// 2. Delete air fill record
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodDelete, "/vehicles/{id}/air-fills/{airFillID}", nil)
		req.AddCookie(cookie)
		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(vehicleID, 10))
		rctx.URLParams.Add("airFillID", strconv.FormatInt(airFillID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.DeleteVehicleAirFill(rec, req)

		if rec.Code != http.StatusNoContent {
			t.Errorf("expected 204 No Content, got %d", rec.Code)
		}

		// Verify deletion
		var count int
		_ = db.QueryRow(`SELECT COUNT(*) FROM vehicle_air_fills WHERE id = $1`, airFillID).Scan(&count)
		if count != 0 {
			t.Errorf("expected air fill to be deleted, got %d", count)
		}
	}

	// 3. Delete fuel fillup record
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodDelete, "/vehicles/{id}/fuel-fillups/{fillupID}", nil)
		req.AddCookie(cookie)
		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(vehicleID, 10))
		rctx.URLParams.Add("fillupID", strconv.FormatInt(fuelFillupID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.DeleteFuelFillup(rec, req)

		if rec.Code != http.StatusNoContent {
			t.Errorf("expected 204 No Content, got %d", rec.Code)
		}

		// Verify deletion
		var count int
		_ = db.QueryRow(`SELECT COUNT(*) FROM vehicle_fuel_fillups WHERE id = $1`, fuelFillupID).Scan(&count)
		if count != 0 {
			t.Errorf("expected fuel fillup to be deleted, got %d", count)
		}
	}
}

func TestMaintenanceRecords(t *testing.T) {
	db, shutdown := testhelper.StartPostgres(t)
	defer shutdown()
	h := NewHandler(db)
	cookie := loginUser(t, db)

	var vehicleID int64
	if err := db.QueryRow(`INSERT INTO vehicles (name) VALUES ('Honda City') RETURNING id`).Scan(&vehicleID); err != nil {
		t.Fatalf("failed to seed vehicle: %v", err)
	}

	request := func(method, path string, body []byte, recordID int64) (*httptest.ResponseRecorder, *http.Request) {
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(method, path, bytes.NewReader(body))
		req.AddCookie(cookie)
		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(vehicleID, 10))
		if recordID != 0 {
			rctx.URLParams.Add("recordID", strconv.FormatInt(recordID, 10))
		}
		return rec, req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))
	}

	payload, _ := json.Marshal(maintenanceRecordPayload{Category: "service", Title: "Annual service", Amount: 4500, OdometerKM: floatPtr(42000)})
	rec, req := request(http.MethodPost, "/vehicles/{id}/maintenance-records", payload, 0)
	h.CreateMaintenanceRecord(rec, req)
	if rec.Code != http.StatusCreated {
		t.Fatalf("expected 201, got %d: %s", rec.Code, rec.Body.String())
	}
	var created maintenanceRecord
	if err := json.NewDecoder(rec.Body).Decode(&created); err != nil {
		t.Fatalf("decode created record: %v", err)
	}
	if created.Category != "service" || created.Amount != 4500 || created.OdometerKM == nil || *created.OdometerKM != 42000 {
		t.Fatalf("unexpected created record: %+v", created)
	}

	updatedPayload, _ := json.Marshal(maintenanceRecordPayload{Category: "repair", Title: "Brake-pad repair", Amount: 5200})
	rec, req = request(http.MethodPut, "/vehicles/{id}/maintenance-records/{recordID}", updatedPayload, created.ID)
	h.UpdateMaintenanceRecord(rec, req)
	if rec.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d: %s", rec.Code, rec.Body.String())
	}
	var updated maintenanceRecord
	_ = json.NewDecoder(rec.Body).Decode(&updated)
	if updated.Category != "repair" || updated.Title != "Brake-pad repair" || updated.Amount != 5200 {
		t.Fatalf("unexpected updated record: %+v", updated)
	}

	rec, req = request(http.MethodGet, "/vehicles/{id}/history", nil, 0)
	h.VehicleHistory(rec, req)
	if rec.Code != http.StatusOK {
		t.Fatalf("expected history to load, got %d", rec.Code)
	}
	var history map[string]any
	_ = json.NewDecoder(rec.Body).Decode(&history)
	records := history["maintenance_records"].([]any)
	if len(records) != 1 {
		t.Fatalf("expected one maintenance record in history, got %v", records)
	}

	rec, req = request(http.MethodDelete, "/vehicles/{id}/maintenance-records/{recordID}", nil, created.ID)
	h.DeleteMaintenanceRecord(rec, req)
	if rec.Code != http.StatusNoContent {
		t.Fatalf("expected 204, got %d", rec.Code)
	}
}

func floatPtr(value float64) *float64 { return &value }
