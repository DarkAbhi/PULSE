package vehicle

import (
	"database/sql"

	"github.com/DarkAbhi/life-backend/internal/vehicle/query"
)

// NewRepository provides this slice's sqlc store without exposing its query package to startup.
func NewRepository(db *sql.DB) *query.Queries { return query.New(db) }
