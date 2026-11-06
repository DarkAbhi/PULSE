package vehicle

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"strings"

	"github.com/DarkAbhi/life-backend/internal/vehicle/query"
)

type Changes struct {
	CatalogID                *string
	MakeName                 *string
	ModelName                *string
	Kind                     *string
	FuelTypes                *[]string
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
	Catalog(context.Context, string) (query.VehicleCatalog, error)
	List(context.Context, int64) ([]query.ListVehiclesRow, error)
	Create(context.Context, query.CreateVehicleParams) (query.CreateVehicleRow, error)
	Fetch(context.Context, int64, int64) (query.GetVehicleRow, error)
	FetchForUpdate(context.Context, int64, int64) (query.GetVehicleForUpdateRow, error)
	Update(context.Context, query.UpdateVehicleParams) (query.UpdateVehicleRow, error)
	UpdateTirePressure(context.Context, query.UpdateVehicleTirePressureParams) (query.UpdateVehicleTirePressureRow, error)
	Delete(context.Context, int64, int64) (int64, error)
}
type Service struct{ store store }

func NewService(store store) *Service { return &Service{store: store} }

func (s *Service) List(ctx context.Context, userID int64) ([]query.ListVehiclesRow, error) {
	rows, err := s.store.List(ctx, userID)
	if err != nil {
		return nil, fmt.Errorf("list vehicles: %w", err)
	}
	return rows, nil
}
func (s *Service) Create(ctx context.Context, userID int64, p Changes) (query.CreateVehicleRow, error) {
	if p.Name == nil || strings.TrimSpace(*p.Name) == "" {
		return query.CreateVehicleRow{}, ValidationError{"name is required"}
	}
	if len([]rune(strings.TrimSpace(*p.Name))) > 48 {
		return query.CreateVehicleRow{}, ValidationError{"name must be at most 48 characters"}
	}
	if p.Kind == nil || p.FuelTypes == nil {
		return query.CreateVehicleRow{}, ValidationError{"kind and fuel_types are required"}
	}
	if err := validateVehicleDetails(*p.Kind, *p.FuelTypes); err != nil {
		return query.CreateVehicleRow{}, err
	}
	catalogID, makeName, modelName, err := s.identity(ctx, *p.Kind, nil, "", "", p)
	if err != nil {
		return query.CreateVehicleRow{}, err
	}
	isActive := true
	if p.IsActive != nil {
		isActive = *p.IsActive
	}
	frontSolo, rearSolo := legacyPressures(p.FrontTirePressureSolo, p.FrontTirePressure), legacyPressures(p.RearTirePressureSolo, p.RearTirePressure)
	if err := validatePressures(frontSolo, rearSolo, p.FrontTirePressurePillion, p.RearTirePressurePillion); err != nil {
		return query.CreateVehicleRow{}, err
	}
	row, err := s.store.Create(ctx, query.CreateVehicleParams{CatalogID: catalogID, MakeName: makeName, ModelName: modelName, Kind: *p.Kind, FuelTypes: *p.FuelTypes, Name: strings.TrimSpace(*p.Name), IsActive: isActive, FrontTirePressureSolo: frontSolo, RearTirePressureSolo: rearSolo, FrontTirePressurePillion: p.FrontTirePressurePillion, RearTirePressurePillion: p.RearTirePressurePillion, UserID: userID})
	if err != nil {
		return query.CreateVehicleRow{}, fmt.Errorf("create vehicle: %w", err)
	}
	return row, nil
}
func (s *Service) Fetch(ctx context.Context, id, userID int64) (query.GetVehicleRow, error) {
	row, err := s.store.Fetch(ctx, id, userID)
	if errors.Is(err, sql.ErrNoRows) {
		return query.GetVehicleRow{}, ErrNotFound
	}
	if err != nil {
		return query.GetVehicleRow{}, fmt.Errorf("get vehicle: %w", err)
	}
	return row, nil
}
func (s *Service) Update(ctx context.Context, id, userID int64, p Changes) (query.UpdateVehicleRow, error) {
	current, err := s.store.FetchForUpdate(ctx, id, userID)
	if errors.Is(err, sql.ErrNoRows) {
		return query.UpdateVehicleRow{}, ErrNotFound
	}
	if err != nil {
		return query.UpdateVehicleRow{}, fmt.Errorf("get vehicle for update: %w", err)
	}
	kind, fuelTypes := current.Kind, current.FuelTypes
	if p.Kind != nil {
		kind = *p.Kind
	}
	if p.FuelTypes != nil {
		fuelTypes = *p.FuelTypes
	}
	if p.Kind != nil && !validKind(kind) {
		return query.UpdateVehicleRow{}, ValidationError{"choose a valid vehicle kind"}
	}
	if err := validateFuelTypes(fuelTypes); err != nil {
		return query.UpdateVehicleRow{}, err
	}
	catalogID, makeName, modelName, err := s.identity(ctx, kind, current.CatalogID, current.MakeName, current.ModelName, p)
	if err != nil {
		return query.UpdateVehicleRow{}, err
	}
	name, isActive := current.Name, current.IsActive
	if p.Name != nil {
		name = strings.TrimSpace(*p.Name)
		if name == "" {
			return query.UpdateVehicleRow{}, ValidationError{"name is required"}
		}
		if len([]rune(name)) > 48 {
			return query.UpdateVehicleRow{}, ValidationError{"name must be at most 48 characters"}
		}
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
	row, err := s.store.Update(ctx, query.UpdateVehicleParams{CatalogID: catalogID, MakeName: makeName, ModelName: modelName, Kind: kind, FuelTypes: fuelTypes, Name: name, IsActive: isActive, FrontTirePressureSolo: frontSolo, RearTirePressureSolo: rearSolo, FrontTirePressurePillion: frontPillion, RearTirePressurePillion: rearPillion, ID: id, UserID: userID})
	if errors.Is(err, sql.ErrNoRows) {
		return query.UpdateVehicleRow{}, ErrNotFound
	}
	if err != nil {
		return query.UpdateVehicleRow{}, fmt.Errorf("update vehicle: %w", err)
	}
	return row, nil
}
func (s *Service) UpdatePressure(ctx context.Context, id, userID int64, p PressureChanges) (query.UpdateVehicleTirePressureRow, error) {
	frontSolo, rearSolo := legacyPressures(p.FrontTirePressureSolo, p.FrontTirePressure), legacyPressures(p.RearTirePressureSolo, p.RearTirePressure)
	if err := validatePressures(frontSolo, rearSolo, p.FrontTirePressurePillion, p.RearTirePressurePillion); err != nil {
		return query.UpdateVehicleTirePressureRow{}, err
	}
	row, err := s.store.UpdateTirePressure(ctx, query.UpdateVehicleTirePressureParams{FrontTirePressureSolo: frontSolo, RearTirePressureSolo: rearSolo, FrontTirePressurePillion: p.FrontTirePressurePillion, RearTirePressurePillion: p.RearTirePressurePillion, ID: id, UserID: userID})
	if errors.Is(err, sql.ErrNoRows) {
		return query.UpdateVehicleTirePressureRow{}, ErrNotFound
	}
	if err != nil {
		return query.UpdateVehicleTirePressureRow{}, fmt.Errorf("update vehicle pressure: %w", err)
	}
	return row, nil
}
func (s *Service) Delete(ctx context.Context, id, userID int64) error {
	n, err := s.store.Delete(ctx, id, userID)
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

func validateVehicleDetails(kind string, fuelTypes []string) error {
	if !validKind(kind) {
		return ValidationError{"choose a valid vehicle kind"}
	}
	return validateFuelTypes(fuelTypes)
}

func validateFuelTypes(fuelTypes []string) error {
	if len(fuelTypes) < 1 || len(fuelTypes) > 2 {
		return ValidationError{"fuel_types must contain one or two fuel types"}
	}
	for _, fuel := range fuelTypes {
		switch fuel {
		case "petrol", "diesel", "lpg", "cng", "electric":
		default:
			return ValidationError{"invalid fuel type"}
		}
	}
	if len(fuelTypes) == 2 && fuelTypes[0] == fuelTypes[1] {
		return ValidationError{"fuel types must be distinct"}
	}
	return nil
}

func validKind(kind string) bool {
	switch kind {
	case "car", "motorcycle", "moped", "van", "truck", "bus":
		return true
	}
	return false
}

// An omitted catalog_id preserves the selection; an empty string clears it.
func (s *Service) identity(ctx context.Context, kind string, id *string, makeName, modelName string, p Changes) (*string, string, string, error) {
	if p.CatalogID != nil {
		if *p.CatalogID == "" {
			id = nil
			makeName = ""
			modelName = ""
		} else {
			id = p.CatalogID
		}
	}
	if id != nil {
		entry, err := s.store.Catalog(ctx, *id)
		if errors.Is(err, sql.ErrNoRows) {
			return nil, "", "", ValidationError{"choose an available catalog model"}
		}
		if err != nil {
			return nil, "", "", fmt.Errorf("resolve vehicle catalog: %w", err)
		}
		if entry.Kind != kind {
			return nil, "", "", ValidationError{"model does not belong to the selected kind"}
		}
		return &entry.ID, entry.MakeName, entry.ModelName, nil
	}
	if p.MakeName != nil {
		makeName = strings.TrimSpace(*p.MakeName)
	}
	if p.ModelName != nil {
		modelName = strings.TrimSpace(*p.ModelName)
	}
	if len([]rune(makeName)) > 200 || len([]rune(modelName)) > 200 {
		return nil, "", "", ValidationError{"make and model must be at most 200 characters"}
	}
	return nil, makeName, modelName, nil
}
