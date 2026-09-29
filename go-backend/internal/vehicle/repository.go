package vehicle

import (
	"context"

	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/DarkAbhi/life-backend/internal/vehicle/query"
)

type Repository struct{ queries *query.Queries }

func NewRepository(db *pgxpool.Pool) *Repository {
	return &Repository{queries: query.New(db)}
}

func (r *Repository) List(ctx context.Context, userID int64) ([]query.ListVehiclesRow, error) {
	return r.queries.ListVehicles(ctx, userID)
}

func (r *Repository) Create(ctx context.Context, arg query.CreateVehicleParams) (query.CreateVehicleRow, error) {
	return r.queries.CreateVehicle(ctx, arg)
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
