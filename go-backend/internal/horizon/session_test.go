package horizon

import (
	"context"
	"database/sql"
	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"github.com/jackc/pgx/v5/pgxpool"
	"net/http"
	"testing"
)

func testSessionLookup(db *sql.DB) SessionLookup {
	return func(r *http.Request) (auth.SessionUser, error) {
		user, err := testhelper.GetSessionUser(db, r)
		return auth.SessionUser{ID: user.ID, Username: user.Username}, err
	}
}

func testPool(t *testing.T, dsn string) *pgxpool.Pool {
	t.Helper()
	pool, err := pgxpool.New(context.Background(), dsn)
	if err != nil {
		t.Fatal(err)
	}
	return pool
}
