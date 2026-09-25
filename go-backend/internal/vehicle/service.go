package vehicle

import (
	"context"
	"database/sql"
	"errors"
	"fmt"

	"github.com/DarkAbhi/life-backend/internal/vehicle/query"
)

type Changes struct {
	Name                     *string
	IsActive                 *bool
	FrontTirePressureSolo    *float64
	RearTirePressureSolo     *float64
	FrontTirePressurePillion *float64
	RearTirePressurePillion  *float64
	FrontTirePressure        *float64
	RearTirePressure         *float64
}

type PressureChanges struct {
	FrontTirePressureSolo    *float64
	RearTirePressureSolo     *float64
	FrontTirePressurePillion *float64
	RearTirePressurePillion  *float64
	FrontTirePressure        *float64
	RearTirePressure         *float64
}

var ErrNotFound = errors.New("vehicle: not found")

type ValidationError struct{ Message string }

func (e ValidationError) Error() string { return e.Message }

type store interface {
	List(context.Context) ([]query.ListVehiclesRow, error)
	Create(context.Context, query.CreateVehicleParams) (query.CreateVehicleRow, error)
	Fetch(context.Context, int64) (query.GetVehicleRow, error)
	FetchForUpdate(context.Context, int64) (query.GetVehicleForUpdateRow, error)
	Update(context.Context, query.UpdateVehicleParams) (query.UpdateVehicleRow, error)
	UpdateTirePressure(context.Context, query.UpdateVehicleTirePressureParams) (query.UpdateVehicleTirePressureRow, error)
	Delete(context.Context, int64) (int64, error)
}
type Service struct{ store store }

func NewService(store store) *Service { return &Service{store: store} }

func (s *Service) List(ctx context.Context) ([]query.ListVehiclesRow, error) {
	rows, err := s.store.List(ctx)
	if err != nil {
		return nil, fmt.Errorf("list vehicles: %w", err)
	}
	return rows, nil
}
func (s *Service) Create(ctx context.Context, p Changes) (query.CreateVehicleRow, error) {
	if p.Name == nil || *p.Name == "" {
		return query.CreateVehicleRow{}, ValidationError{"name is required"}
	}
	isActive := true
	if p.IsActive != nil {
		isActive = *p.IsActive
	}
	frontSolo, rearSolo := legacyPressures(p.FrontTirePressureSolo, p.FrontTirePressure), legacyPressures(p.RearTirePressureSolo, p.RearTirePressure)
	if err := validatePressures(frontSolo, rearSolo, p.FrontTirePressurePillion, p.RearTirePressurePillion); err != nil {
		return query.CreateVehicleRow{}, err
	}
	row, err := s.store.Create(ctx, query.CreateVehicleParams{Name: *p.Name, IsActive: isActive, FrontTirePressureSolo: frontSolo, RearTirePressureSolo: rearSolo, FrontTirePressurePillion: p.FrontTirePressurePillion, RearTirePressurePillion: p.RearTirePressurePillion})
	if err != nil {
		return query.CreateVehicleRow{}, fmt.Errorf("create vehicle: %w", err)
	}
	return row, nil
}
func (s *Service) Fetch(ctx context.Context, id int64) (query.GetVehicleRow, error) {
	row, err := s.store.Fetch(ctx, id)
	if errors.Is(err, sql.ErrNoRows) {
		return query.GetVehicleRow{}, ErrNotFound
	}
	if err != nil {
		return query.GetVehicleRow{}, fmt.Errorf("get vehicle: %w", err)
	}
	return row, nil
}
func (s *Service) Update(ctx context.Context, id int64, p Changes) (query.UpdateVehicleRow, error) {
	current, err := s.store.FetchForUpdate(ctx, id)
	if errors.Is(err, sql.ErrNoRows) {
		return query.UpdateVehicleRow{}, ErrNotFound
	}
	if err != nil {
		return query.UpdateVehicleRow{}, fmt.Errorf("get vehicle for update: %w", err)
	}
	name, isActive := current.Name, current.IsActive
	if p.Name != nil {
		name = *p.Name
	}
	if p.IsActive != nil {
		isActive = *p.IsActive
	}
	frontSolo, rearSolo := current.FrontTirePressureSolo, current.RearTirePressureSolo
	frontPillion, rearPillion := current.FrontTirePressurePillion, current.RearTirePressurePillion
	if v := legacyPressures(p.FrontTirePressureSolo, p.FrontTirePressure); v != nil {
		frontSolo = v
	}
	if v := legacyPressures(p.RearTirePressureSolo, p.RearTirePressure); v != nil {
		rearSolo = v
	}
	if p.FrontTirePressurePillion != nil {
		frontPillion = p.FrontTirePressurePillion
	}
	if p.RearTirePressurePillion != nil {
		rearPillion = p.RearTirePressurePillion
	}
	if err := validatePressures(frontSolo, rearSolo, frontPillion, rearPillion); err != nil {
		return query.UpdateVehicleRow{}, err
	}
	row, err := s.store.Update(ctx, query.UpdateVehicleParams{Name: name, IsActive: isActive, FrontTirePressureSolo: frontSolo, RearTirePressureSolo: rearSolo, FrontTirePressurePillion: frontPillion, RearTirePressurePillion: rearPillion, ID: id})
	if err != nil {
		return query.UpdateVehicleRow{}, fmt.Errorf("update vehicle: %w", err)
	}
	return row, nil
}
func (s *Service) UpdatePressure(ctx context.Context, id int64, p PressureChanges) (query.UpdateVehicleTirePressureRow, error) {
	frontSolo, rearSolo := legacyPressures(p.FrontTirePressureSolo, p.FrontTirePressure), legacyPressures(p.RearTirePressureSolo, p.RearTirePressure)
	if err := validatePressures(frontSolo, rearSolo, p.FrontTirePressurePillion, p.RearTirePressurePillion); err != nil {
		return query.UpdateVehicleTirePressureRow{}, err
	}
	row, err := s.store.UpdateTirePressure(ctx, query.UpdateVehicleTirePressureParams{FrontTirePressureSolo: frontSolo, RearTirePressureSolo: rearSolo, FrontTirePressurePillion: p.FrontTirePressurePillion, RearTirePressurePillion: p.RearTirePressurePillion, ID: id})
	if errors.Is(err, sql.ErrNoRows) {
		return query.UpdateVehicleTirePressureRow{}, ErrNotFound
	}
	if err != nil {
		return query.UpdateVehicleTirePressureRow{}, fmt.Errorf("update vehicle pressure: %w", err)
	}
	return row, nil
}
func (s *Service) Delete(ctx context.Context, id int64) error {
	n, err := s.store.Delete(ctx, id)
	if err != nil {
		return fmt.Errorf("delete vehicle: %w", err)
	}
	if n == 0 {
		return ErrNotFound
	}
	return nil
}
func legacyPressures(current, legacy *float64) *float64 {
	if current != nil {
		return current
	}
	return legacy
}
func validatePressures(frontSolo, rearSolo, frontPillion, rearPillion *float64) error {
	for _, item := range []struct {
		value   *float64
		message string
	}{
		{frontSolo, "front solo tire pressure cannot be negative"},
		{rearSolo, "rear solo tire pressure cannot be negative"},
		{frontPillion, "front pillion tire pressure cannot be negative"},
		{rearPillion, "rear pillion tire pressure cannot be negative"},
	} {
		if item.value != nil && *item.value < 0 {
			return ValidationError{item.message}
		}
	}
	return nil
}
