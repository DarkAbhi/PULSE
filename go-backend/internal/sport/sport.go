package sport

import (
	"database/sql"
	"encoding/json"
	"net/http"

	"github.com/DarkAbhi/life-backend/internal/db/sqlc"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type addSportBody struct {
	Sport string `json:"sport"`
}

type Handler struct {
	DB *sql.DB
}

func NewHandler(db *sql.DB) *Handler {
	return &Handler{DB: db}
}

// AddSportForDay records a sport session for today.
func (h *Handler) AddSportForDay(w http.ResponseWriter, r *http.Request) {
	var body addSportBody
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		webutil.BadRequest(w, "Invalid JSON.")
		return
	}
	switch body.Sport {
	case "cricket", "football", "badminton":
		err := sqlc.New(h.DB).AddSport(r.Context(), body.Sport)
		if err != nil {
			webutil.ServerError(w, err)
			return
		}
		webutil.WriteJSON(w, http.StatusCreated, map[string]string{"message": "success"})
	default:
		webutil.BadRequest(w, "This sport is not available yet.")
	}
}
