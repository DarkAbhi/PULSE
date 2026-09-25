package horizon

import (
	"database/sql"
	"github.com/DarkAbhi/life-backend/internal/auth"
	"net/http"
)

func testSessionLookup(db *sql.DB) SessionLookup {
	return func(r *http.Request) (auth.SessionUser, error) { return auth.GetSessionUser(db, r) }
}
