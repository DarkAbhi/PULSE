package vehicle

import (
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"reflect"
	"strings"
	"testing"

	"github.com/go-chi/chi/v5"

	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/DarkAbhi/life-backend/internal/vehicle/query"
)

type stubVehicleStore struct {
	created      query.CreateVehicleParams
	updated      query.UpdateVehicleParams
	deletedCount int64
	deleteErr    error
}

func (s *stubVehicleStore) List(context.Context, int64) ([]query.ListVehiclesRow, error) {
	return nil, nil
}

func (s *stubVehicleStore) Create(_ context.Context, arg query.CreateVehicleParams) (query.CreateVehicleRow, error) {
	s.created = arg
	return query.CreateVehicleRow{ID: 42, Name: arg.Name, VehicleType: arg.VehicleType, FuelTypes: arg.FuelTypes}, nil
}

func (s *stubVehicleStore) Fetch(context.Context, int64, int64) (query.GetVehicleRow, error) {
	return query.GetVehicleRow{}, nil
}

func (s *stubVehicleStore) FetchForUpdate(context.Context, int64, int64) (query.GetVehicleForUpdateRow, error) {
	return query.GetVehicleForUpdateRow{Name: "Scooter", VehicleType: "two_wheeler", FuelTypes: []string{"petrol"}}, nil
}

func (s *stubVehicleStore) Update(_ context.Context, arg query.UpdateVehicleParams) (query.UpdateVehicleRow, error) {
	s.updated = arg
	return query.UpdateVehicleRow{VehicleType: arg.VehicleType, FuelTypes: arg.FuelTypes}, nil
}

func (s *stubVehicleStore) UpdateTirePressure(context.Context, query.UpdateVehicleTirePressureParams) (query.UpdateVehicleTirePressureRow, error) {
	return query.UpdateVehicleTirePressureRow{}, nil
}

func (s *stubVehicleStore) Delete(context.Context, int64, int64) (int64, error) {
	if s.deleteErr != nil {
		return 0, s.deleteErr
	}
	return s.deletedCount, nil
}

func TestService_Delete(t *testing.T) {
	tests := []struct {
		name         string
		deletedCount int64
		storeErr     error
		expectedErr  error
	}{
		{
			name:         "successful deletion",
			deletedCount: 1,
			expectedErr:  nil,
		},
		{
			name:         "vehicle not found",
			deletedCount: 0,
			expectedErr:  ErrNotFound,
		},
		{
			name:        "store error",
			storeErr:    errors.New("db connection failure"),
			expectedErr: errors.New("delete vehicle: db connection failure"),
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			store := &stubVehicleStore{
				deletedCount: tt.deletedCount,
				deleteErr:    tt.storeErr,
			}
			service := NewService(store)

			err := service.Delete(context.Background(), 42, 1)
			if tt.expectedErr == nil {
				if err != nil {
					t.Fatalf("expected nil error, got %v", err)
				}
			} else {
				if err == nil {
					t.Fatalf("expected error %v, got nil", tt.expectedErr)
				}
				if errors.Is(tt.expectedErr, ErrNotFound) && !errors.Is(err, ErrNotFound) {
					t.Fatalf("expected ErrNotFound, got %v", err)
				}
				if tt.storeErr != nil && err.Error() != tt.expectedErr.Error() {
					t.Fatalf("expected %v, got %v", tt.expectedErr, err)
				}
			}
		})
	}
}

func TestDeleteVehicleRouteRegistration(t *testing.T) {
	store := &stubVehicleStore{deletedCount: 1}
	service := NewService(store)
	handler := NewHandler(nil, service, func(*http.Request) (auth.SessionUser, error) { return auth.SessionUser{ID: 1}, nil })

	r := chi.NewRouter()
	handler.RegisterRoutes(r)

	req := httptest.NewRequest(http.MethodDelete, "/vehicles/42", nil)
	rec := httptest.NewRecorder()
	r.ServeHTTP(rec, req)

	if rec.Code != http.StatusNoContent {
		t.Fatalf("expected status 204 No Content, got %d", rec.Code)
	}
}

func TestCreateVehicleDetails(t *testing.T) {
	for _, tc := range []struct {
		name, body string
		status     int
	}{
		{"missing details", `{"name":"Scooter"}`, 400},
		{"null details", `{"name":"Scooter","vehicle_type":null,"fuel_types":null}`, 400},
		{"missing fuels", `{"name":"Scooter","vehicle_type":"two_wheeler"}`, 400},
		{"invalid vehicle", `{"name":"Scooter","vehicle_type":"truck","fuel_types":["petrol"]}`, 400},
		{"empty fuels", `{"name":"Scooter","vehicle_type":"two_wheeler","fuel_types":[]}`, 400},
		{"invalid fuel", `{"name":"Scooter","vehicle_type":"two_wheeler","fuel_types":["water"]}`, 400},
		{"duplicate fuels", `{"name":"Car","vehicle_type":"four_wheeler","fuel_types":["petrol","petrol"]}`, 400},
		{"too many fuels", `{"name":"Car","vehicle_type":"four_wheeler","fuel_types":["petrol","cng","lpg"]}`, 400},
		{"single tank", `{"name":"Scooter","vehicle_type":"two_wheeler","fuel_types":["petrol"]}`, 201},
		{"dual tank", `{"name":"Car","vehicle_type":"four_wheeler","fuel_types":["petrol","cng"]}`, 201},
	} {
		t.Run(tc.name, func(t *testing.T) {
			store := &stubVehicleStore{}
			h := NewHandler(nil, NewService(store), func(*http.Request) (auth.SessionUser, error) { return auth.SessionUser{ID: 7}, nil })
			rec := httptest.NewRecorder()
			h.Create(rec, httptest.NewRequest(http.MethodPost, "/vehicles", strings.NewReader(tc.body)))
			if rec.Code != tc.status {
				t.Fatalf("status %d, want %d: %s", rec.Code, tc.status, rec.Body.String())
			}
			if tc.status == 201 {
				var out DTO
				if err := json.Unmarshal(rec.Body.Bytes(), &out); err != nil {
					t.Fatal(err)
				}
				if out.VehicleType != store.created.VehicleType || !reflect.DeepEqual(out.FuelTypes, store.created.FuelTypes) || store.created.UserID != 7 {
					t.Fatalf("details not preserved: %+v, %+v", out, store.created)
				}
			} else if store.created.Name != "" {
				t.Fatal("invalid vehicle reached storage")
			}
		})
	}
}

func TestUpdateVehicleDetails(t *testing.T) {
	store := &stubVehicleStore{}
	service := NewService(store)
	// Unrelated partial updates must preserve mandatory details.
	if _, err := service.Update(context.Background(), 42, 7, Changes{}); err != nil {
		t.Fatal(err)
	}
	if store.updated.VehicleType != "two_wheeler" || !reflect.DeepEqual(store.updated.FuelTypes, []string{"petrol"}) {
		t.Fatalf("details lost: %+v", store.updated)
	}
	vehicleType, fuels := "four_wheeler", []string{"petrol", "cng"}
	if _, err := service.Update(context.Background(), 42, 7, Changes{VehicleType: &vehicleType, FuelTypes: &fuels}); err != nil {
		t.Fatal(err)
	}
	if store.updated.VehicleType != vehicleType || !reflect.DeepEqual(store.updated.FuelTypes, fuels) {
		t.Fatalf("details not updated: %+v", store.updated)
	}
	empty := []string{}
	if _, err := service.Update(context.Background(), 42, 7, Changes{FuelTypes: &empty}); err == nil {
		t.Fatal("empty fuels accepted")
	}
}
