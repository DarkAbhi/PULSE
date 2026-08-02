package budgets

import (
	"database/sql"
	"encoding/json"
	"errors"
	"math"
	"net/http"
	"strings"

	"github.com/DarkAbhi/life-backend/internal/auth"
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

type Handler struct {
	DB *sql.DB
}

func NewHandler(db *sql.DB) *Handler {
	return &Handler{DB: db}
}

func (h *Handler) FetchBudgets(userID int64) ([]BudgetDTO, float64, error) {
	rows, err := h.DB.Query(`SELECT id, name, allocated_amount FROM financial_horizon_budgets WHERE user_id = $1 ORDER BY created_at ASC, id ASC`, userID)
	if err != nil {
		return nil, 0, err
	}
	defer rows.Close()

	budgetsList := make([]BudgetDTO, 0)
	var totalBudgetsAllocated float64

	for rows.Next() {
		var b BudgetDTO
		if err := rows.Scan(&b.ID, &b.Name, &b.AllocatedAmount); err != nil {
			return nil, 0, err
		}
		totalBudgetsAllocated += b.AllocatedAmount
		budgetsList = append(budgetsList, b)
	}

	return budgetsList, totalBudgetsAllocated, nil
}

func (h *Handler) CreateBudget(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
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
		webutil.BadRequest(w, "invalid JSON")
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

	var b BudgetDTO
	err = h.DB.QueryRow(`
		INSERT INTO financial_horizon_budgets (user_id, name, allocated_amount)
		VALUES ($1, $2, $3)
		RETURNING id, name, allocated_amount
	`, user.ID, in.Name, in.AllocatedAmount).Scan(&b.ID, &b.Name, &b.AllocatedAmount)

	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	b.UsedAmount = 0
	b.AvailableAmount = b.AllocatedAmount
	b.UsagePercentage = 0

	webutil.WriteJSON(w, http.StatusCreated, b)
}

func (h *Handler) UpdateBudget(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
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
		webutil.BadRequest(w, "invalid JSON")
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

	var b BudgetDTO
	err = h.DB.QueryRow(`
		UPDATE financial_horizon_budgets
		SET name = $1, allocated_amount = $2, updated_at = CURRENT_TIMESTAMP
		WHERE id = $3 AND user_id = $4
		RETURNING id, name, allocated_amount
	`, in.Name, in.AllocatedAmount, budgetID, user.ID).Scan(&b.ID, &b.Name, &b.AllocatedAmount)

	if errors.Is(err, sql.ErrNoRows) {
		http.NotFound(w, r)
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	var usedAmount float64
	err = h.DB.QueryRow(`
		SELECT COALESCE(SUM(amount), 0)
		FROM financial_horizon_deductions
		WHERE budget_id = $1 AND user_id = $2 AND is_active = true
	`, budgetID, user.ID).Scan(&usedAmount)

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

func (h *Handler) DeleteBudget(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
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

	result, err := h.DB.Exec(`DELETE FROM financial_horizon_budgets WHERE id = $1 AND user_id = $2`, budgetID, user.ID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	deleted, err := result.RowsAffected()
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
