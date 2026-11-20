package vehicle

import (
	"context"

	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/DarkAbhi/life-backend/internal/vehicle/query"
)

type Repository struct {
	queries *query.Queries
	db      *pgxpool.Pool
}

func NewRepository(db *pgxpool.Pool) *Repository {
	return &Repository{queries: query.New(db), db: db}
}

func (r *Repository) Catalog(ctx context.Context, id string) (query.VehicleCatalog, error) {
	return r.queries.GetCatalogModel(ctx, id)
}

func (r *Repository) List(ctx context.Context, userID int64) ([]query.ListVehiclesRow, error) {
	return r.queries.ListVehicles(ctx, userID)
}

func (r *Repository) Create(ctx context.Context, arg query.CreateVehicleParams) (query.CreateVehicleRow, error) {
	tx, err := r.db.Begin(ctx)
	if err != nil {
		return query.CreateVehicleRow{}, err
	}
	defer tx.Rollback(ctx)
	q := query.New(tx)
	row, err := q.CreateVehicle(ctx, arg)
	if err != nil {
		return query.CreateVehicleRow{}, err
	}
	for _, setting := range startingSchedules(row.Kind) {
		if setting.Kind == "engine_oil" && len(row.FuelTypes) == 1 && row.FuelTypes[0] == "electric" {
			setting.Personal = scheduleInterval{Unit: "months"}
		}
		_, err = q.SaveMaintenanceSchedule(ctx, query.SaveMaintenanceScheduleParams{VehicleID: row.ID, UserID: arg.UserID, Kind: setting.Kind, ActiveSource: "personal", PersonalKm: setting.Personal.Kilometers, PersonalDuration: setting.Personal.Duration, PersonalUnit: setting.Personal.Unit, ManualUnit: "months"})
		if err != nil {
			return query.CreateVehicleRow{}, err
		}
	}
	if err := tx.Commit(ctx); err != nil {
		return query.CreateVehicleRow{}, err
	}
	return row, nil
}

func (r *Repository) Fetch(ctx context.Context, id, userID int64) (query.GetVehicleRow, error) {
	return r.queries.GetVehicle(ctx, query.GetVehicleParams{ID: id, UserID: userID})
}

func (r *Repository) FetchForUpdate(ctx context.Context, id, userID int64) (query.GetVehicleForUpdateRow, error) {
	return r.queries.GetVehicleForUpdate(ctx, query.GetVehicleForUpdateParams{ID: id, UserID: userID})
}

func (r *Repository) Update(ctx context.Context, arg query.UpdateVehicleParams) (query.UpdateVehicleRow, error) {
	return r.queries.UpdateVehicle(ctx, arg)
}

func (r *Repository) UpdateTirePressure(ctx context.Context, arg query.UpdateVehicleTirePressureParams) (query.UpdateVehicleTirePressureRow, error) {
	return r.queries.UpdateVehicleTirePressure(ctx, arg)
}

func (r *Repository) Delete(ctx context.Context, id, userID int64) (int64, error) {
	return r.queries.DeleteVehicle(ctx, query.DeleteVehicleParams{ID: id, UserID: userID})
}
