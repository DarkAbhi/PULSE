package vehicle

import (
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/DarkAbhi/life-backend/internal/vehicle/query"
)

// NewRepository provides this slice's sqlc store without exposing its query package to startup.
func NewRepository(db *pgxpool.Pool) *query.Queries { return query.New(db) }
