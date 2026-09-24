package purchase

import (
	"database/sql"
	"encoding/json"
	"errors"
	"net/http"
	"strings"

	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/DarkAbhi/life-backend/internal/db/sqlc"
	"github.com/DarkAbhi/life-backend/internal/timeutil"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type purchaseInput struct {
	Name  string  `json:"name"`
	Price float64 `json:"price"`
	URL   *string `json:"url"`
}

type purchaseDTO struct {
	ID    int64   `json:"id"`
	Name  string  `json:"name"`
	Price float64 `json:"price"`
	URL   *string `json:"url"`
}

type Handler struct {
	DB *sql.DB
}

func NewHandler(db *sql.DB) *Handler {
	return &Handler{DB: db}
}

func (h *Handler) NextMonthPurchases(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	month := timeutil.NextMonthDate()
	rows, err := sqlc.New(h.DB).ListNextMonthPurchases(r.Context(), sqlc.ListNextMonthPurchasesParams{UserID: user.ID, Column2: month})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	items := make([]purchaseDTO, 0)
	total := 0.0
	for _, row := range rows {
		item := purchaseDTO{ID: row.ID, Name: row.Name, Price: row.Price}
		if row.Url.Valid {
			item.URL = &row.Url.String
		}
		total += item.Price
		items = append(items, item)
	}
	webutil.WriteJSON(w, http.StatusOK, map[string]any{"items": items, "total": total})
}

func (h *Handler) CreateNextMonthPurchase(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	var in purchaseInput
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<20)).Decode(&in); err != nil {
		webutil.BadRequest(w, "invalid JSON")
		return
	}
	in.Name = strings.TrimSpace(in.Name)
	if in.Name == "" || len([]rune(in.Name)) > 160 || in.Price < 0 {
		webutil.BadRequest(w, "name and a valid price are required")
		return
	}
	url := sql.NullString{}
	if in.URL != nil {
		url = sql.NullString{String: *in.URL, Valid: true}
	}
	row, err := sqlc.New(h.DB).CreateNextMonthPurchase(r.Context(), sqlc.CreateNextMonthPurchaseParams{UserID: user.ID, Column2: timeutil.NextMonthDate(), Name: in.Name, Price: in.Price, Url: url})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	item := purchaseDTO{ID: row.ID, Name: row.Name, Price: row.Price}
	if row.Url.Valid {
		item.URL = &row.Url.String
	}
	webutil.WriteJSON(w, http.StatusCreated, item)
}

func (h *Handler) DeleteNextMonthPurchase(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	purchaseID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	deleted, err := sqlc.New(h.DB).DeleteNextMonthPurchase(r.Context(), sqlc.DeleteNextMonthPurchaseParams{ID: purchaseID, UserID: user.ID, Column3: timeutil.NextMonthDate()})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	if deleted == 0 {
		http.NotFound(w, r)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) ClearNextMonthPurchases(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	if err := sqlc.New(h.DB).ClearNextMonthPurchases(r.Context(), sqlc.ClearNextMonthPurchasesParams{UserID: user.ID, Column2: timeutil.NextMonthDate()}); err != nil {
		webutil.ServerError(w, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
