package horizon

import (
	"context"
	"database/sql"
	"encoding/json"
	"errors"
	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/jackc/pgx/v5/pgxpool"
	"math"
	"net/http"
	"strings"

	"github.com/DarkAbhi/life-backend/internal/horizon/query"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type BudgetInput struct {
	Name            string  `json:"name"`
	AllocatedAmount float64 `json:"allocated_amount"`
}

type BudgetDTO struct {
	ID              int64   `json:"id"`
	Name            string  `json:"name"`
	AllocatedAmount float64 `json:"allocated_amount"`
	UsedAmount      float64 `json:"used_amount"`
	AvailableAmount float64 `json:"available_amount"`
	UsagePercentage float64 `json:"usage_percentage"`
}

type BudgetsHandler struct {
	sessions SessionLookup
	DB       *pgxpool.Pool
}

func NewBudgetsHandler(db *pgxpool.Pool, sessions SessionLookup) *BudgetsHandler {
	return &BudgetsHandler{DB: db, sessions: sessions}
}
func (h *BudgetsHandler) sessionUser(r *http.Request) (auth.SessionUser, error) { return h.sessions(r) }

func (h *BudgetsHandler) FetchBudgets(userID int64) ([]BudgetDTO, float64, error) {
	rows, err := query.New(h.DB).ListBudgets(context.Background(), userID)
	if err != nil {
		return nil, 0, err
	}

	budgetsList := make([]BudgetDTO, 0)
	var totalBudgetsAllocated float64

	for _, row := range rows {
		b := BudgetDTO{ID: row.ID, Name: row.Name, AllocatedAmount: row.AllocatedAmount}
		totalBudgetsAllocated += b.AllocatedAmount
		budgetsList = append(budgetsList, b)
	}

	return budgetsList, totalBudgetsAllocated, nil
}

func (h *BudgetsHandler) CreateBudget(w http.ResponseWriter, r *http.Request) {
	user, err := h.sessionUser(r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	var in BudgetInput
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<20)).Decode(&in); err != nil {
		webutil.BadRequest(w, "invalid json")
		return
	}

	in.Name = strings.TrimSpace(in.Name)
	if in.Name == "" || len([]rune(in.Name)) > 160 {
		webutil.BadRequest(w, "name is required and must be under 160 characters")
		return
	}

	if in.AllocatedAmount < 0 {
		webutil.BadRequest(w, "allocated amount cannot be negative")
		return
	}

	row, err := query.New(h.DB).CreateBudget(r.Context(), query.CreateBudgetParams{UserID: user.ID, Name: in.Name, AllocatedAmount: in.AllocatedAmount})

	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	b := BudgetDTO{ID: row.ID, Name: row.Name, AllocatedAmount: row.AllocatedAmount}
	b.UsedAmount = 0
	b.AvailableAmount = b.AllocatedAmount
	b.UsagePercentage = 0

	webutil.WriteJSON(w, http.StatusCreated, b)
}

func (h *BudgetsHandler) UpdateBudget(w http.ResponseWriter, r *http.Request) {
	user, err := h.sessionUser(r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	budgetID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}

	var in BudgetInput
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<20)).Decode(&in); err != nil {
		webutil.BadRequest(w, "invalid json")
		return
	}

	in.Name = strings.TrimSpace(in.Name)
	if in.Name == "" || len([]rune(in.Name)) > 160 {
		webutil.BadRequest(w, "name is required and must be under 160 characters")
		return
	}

	if in.AllocatedAmount < 0 {
		webutil.BadRequest(w, "allocated amount cannot be negative")
		return
	}

	row, err := query.New(h.DB).UpdateBudget(r.Context(), query.UpdateBudgetParams{Name: in.Name, AllocatedAmount: in.AllocatedAmount, ID: budgetID, UserID: user.ID})

	if errors.Is(err, sql.ErrNoRows) {
		http.NotFound(w, r)
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	b := BudgetDTO{ID: row.ID, Name: row.Name, AllocatedAmount: row.AllocatedAmount}
	usedAmount, err := query.New(h.DB).GetBudgetUsedAmount(r.Context(), query.GetBudgetUsedAmountParams{BudgetID: sql.NullInt64{Int64: budgetID, Valid: true}, UserID: user.ID})

	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	b.UsedAmount = usedAmount
	b.AvailableAmount = b.AllocatedAmount - b.UsedAmount
	if b.AllocatedAmount > 0 {
		b.UsagePercentage = math.Round((b.UsedAmount/b.AllocatedAmount)*10000) / 100
	}

	webutil.WriteJSON(w, http.StatusOK, b)
}

func (h *BudgetsHandler) DeleteBudget(w http.ResponseWriter, r *http.Request) {
	user, err := h.sessionUser(r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	budgetID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}

	deleted, err := query.New(h.DB).DeleteBudget(r.Context(), query.DeleteBudgetParams{ID: budgetID, UserID: user.ID})
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
