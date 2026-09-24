package meditation

import (
	"database/sql"
	"errors"
	"net/http"
	"time"

	"github.com/DarkAbhi/life-backend/internal/db/sqlc"
	"github.com/DarkAbhi/life-backend/internal/timeutil"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type Handler struct {
	DB *sql.DB
}

func NewHandler(db *sql.DB) *Handler {
	return &Handler{DB: db}
}

// AddMeditationForDay records a meditation session for today.
func (h *Handler) AddMeditationForDay(w http.ResponseWriter, r *http.Request) {
	start, end := timeutil.DayBoundsIndia(time.Now().UTC())
	_, err := sqlc.New(h.DB).GetMeditationToday(r.Context(), sqlc.GetMeditationTodayParams{CreatedAt: start, CreatedAt_2: end})
	switch {
	case err == nil:
		webutil.BadRequest(w, "You have already meditated today.")
		return
	case errors.Is(err, sql.ErrNoRows):
		err := sqlc.New(h.DB).AddMeditation(r.Context())
		if err != nil {
			webutil.ServerError(w, err)
			return
		}
		webutil.WriteJSON(w, http.StatusCreated, map[string]string{"message": "success"})
		return
	default:
		webutil.ServerError(w, err)
		return
	}
}
