package vehicle

import (
	"context"
	"errors"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/go-chi/chi/v5"

	"github.com/DarkAbhi/life-backend/internal/vehicle/query"
)

type stubVehicleStore struct {
	deletedCount int64
	deleteErr    error
}

func (s *stubVehicleStore) List(context.Context) ([]query.ListVehiclesRow, error) {
	return nil, nil
}

func (s *stubVehicleStore) Create(context.Context, query.CreateVehicleParams) (query.CreateVehicleRow, error) {
	return query.CreateVehicleRow{}, nil
}

func (s *stubVehicleStore) Fetch(context.Context, int64) (query.GetVehicleRow, error) {
	return query.GetVehicleRow{}, nil
}

func (s *stubVehicleStore) FetchForUpdate(context.Context, int64) (query.GetVehicleForUpdateRow, error) {
	return query.GetVehicleForUpdateRow{}, nil
}

func (s *stubVehicleStore) Update(context.Context, query.UpdateVehicleParams) (query.UpdateVehicleRow, error) {
	return query.UpdateVehicleRow{}, nil
}

func (s *stubVehicleStore) UpdateTirePressure(context.Context, query.UpdateVehicleTirePressureParams) (query.UpdateVehicleTirePressureRow, error) {
	return query.UpdateVehicleTirePressureRow{}, nil
}

func (s *stubVehicleStore) Delete(context.Context, int64) (int64, error) {
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

			err := service.Delete(context.Background(), 42)
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
	handler := NewHandler(nil, service, nil)

	r := chi.NewRouter()
	handler.RegisterRoutes(r)

	req := httptest.NewRequest(http.MethodDelete, "/vehicles/42", nil)
	rec := httptest.NewRecorder()
	r.ServeHTTP(rec, req)

	if rec.Code != http.StatusNoContent {
		t.Fatalf("expected status 204 No Content, got %d", rec.Code)
	}
}
