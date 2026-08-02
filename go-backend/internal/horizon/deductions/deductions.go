package deductions

import (
	"database/sql"
	"encoding/json"
	"errors"
	"net/http"
	"strings"

	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type DeductionInput struct {
	Name     string  `json:"name"`
	Category string  `json:"category"`
	Amount   float64 `json:"amount"`
	DueDay   *int    `json:"due_day"`
	IsActive *bool   `json:"is_active"`
	BudgetID *int64  `json:"budget_id"`
}

type DeductionDTO struct {
	ID       int64   `json:"id"`
	Name     string  `json:"name"`
	Category string  `json:"category"`
	Amount   float64 `json:"amount"`
	DueDay   *int    `json:"due_day"`
	IsActive bool    `json:"is_active"`
	BudgetID *int64  `json:"budget_id"`
}

type Handler struct {
	DB *sql.DB
}

func NewHandler(db *sql.DB) *Handler {
	return &Handler{DB: db}
}

var ValidCategories = map[string]bool{
	"housing":      true,
	"investment":   true,
	"bill":         true,
	"subscription": true,
	"debt":         true,
	"other":        true,
}

func (h *Handler) FetchDeductions(userID int64, budgetUsedMap map[int64]float64) ([]DeductionDTO, float64, error) {
	rows, err := h.DB.Query(`SELECT id, name, category, amount, due_day, is_active, budget_id FROM financial_horizon_deductions WHERE user_id = $1 ORDER BY is_active DESC, created_at DESC, id DESC`, userID)
	if err != nil {
		return nil, 0, err
	}
	defer rows.Close()

	deductions := make([]DeductionDTO, 0)
	var totalDeductions float64

	for rows.Next() {
		var item DeductionDTO
		var dueDay sql.NullInt64
		var budgetID sql.NullInt64

		if err := rows.Scan(&item.ID, &item.Name, &item.Category, &item.Amount, &dueDay, &item.IsActive, &budgetID); err != nil {
			return nil, 0, err
		}
		if dueDay.Valid {
			d := int(dueDay.Int64)
			item.DueDay = &d
		}
		if budgetID.Valid {
			bID := budgetID.Int64
			item.BudgetID = &bID
			if item.IsActive && budgetUsedMap != nil {
				budgetUsedMap[bID] += item.Amount
			}
		}

		if item.IsActive {
			totalDeductions += item.Amount
		}
		deductions = append(deductions, item)
	}

	return deductions, totalDeductions, nil
}

func (h *Handler) CreateDeduction(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	var in DeductionInput
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<20)).Decode(&in); err != nil {
		webutil.BadRequest(w, "invalid JSON")
		return
	}

	in.Name = strings.TrimSpace(in.Name)
	if in.Name == "" || len([]rune(in.Name)) > 160 {
		webutil.BadRequest(w, "name is required and must be under 160 characters")
		return
	}

	if in.Amount < 0 {
		webutil.BadRequest(w, "amount cannot be negative")
		return
	}

	category := strings.ToLower(strings.TrimSpace(in.Category))
	if !ValidCategories[category] {
		category = "bill"
	}

	if in.DueDay != nil && (*in.DueDay < 1 || *in.DueDay > 31) {
		webutil.BadRequest(w, "due day must be between 1 and 31")
		return
	}

	isActive := true
	if in.IsActive != nil {
		isActive = *in.IsActive
	}

	var budgetID sql.NullInt64
	if in.BudgetID != nil {
		budgetID = sql.NullInt64{Int64: *in.BudgetID, Valid: true}
	}

	var item DeductionDTO
	var dueDay sql.NullInt64
	if in.DueDay != nil {
		dueDay = sql.NullInt64{Int64: int64(*in.DueDay), Valid: true}
	}

	err = h.DB.QueryRow(`
		INSERT INTO financial_horizon_deductions (user_id, name, category, amount, due_day, is_active, budget_id)
		VALUES ($1, $2, $3, $4, $5, $6, $7)
		RETURNING id, name, category, amount, due_day, is_active, budget_id
	`, user.ID, in.Name, category, in.Amount, dueDay, isActive, budgetID).Scan(&item.ID, &item.Name, &item.Category, &item.Amount, &dueDay, &item.IsActive, &budgetID)

	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	if dueDay.Valid {
		d := int(dueDay.Int64)
		item.DueDay = &d
	}
	if budgetID.Valid {
		bID := budgetID.Int64
		item.BudgetID = &bID
	}

	webutil.WriteJSON(w, http.StatusCreated, item)
}

func (h *Handler) UpdateDeduction(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	deductionID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}

	var in DeductionInput
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<20)).Decode(&in); err != nil {
		webutil.BadRequest(w, "invalid JSON")
		return
	}

	in.Name = strings.TrimSpace(in.Name)
	if in.Name == "" || len([]rune(in.Name)) > 160 {
		webutil.BadRequest(w, "name is required and must be under 160 characters")
		return
	}

	if in.Amount < 0 {
		webutil.BadRequest(w, "amount cannot be negative")
		return
	}

	category := strings.ToLower(strings.TrimSpace(in.Category))
	if !ValidCategories[category] {
		category = "bill"
	}

	if in.DueDay != nil && (*in.DueDay < 1 || *in.DueDay > 31) {
		webutil.BadRequest(w, "due day must be between 1 and 31")
		return
	}

	isActive := true
	if in.IsActive != nil {
		isActive = *in.IsActive
	}

	var budgetID sql.NullInt64
	if in.BudgetID != nil {
		budgetID = sql.NullInt64{Int64: *in.BudgetID, Valid: true}
	}

	var item DeductionDTO
	var dueDay sql.NullInt64
	if in.DueDay != nil {
		dueDay = sql.NullInt64{Int64: int64(*in.DueDay), Valid: true}
	}

	err = h.DB.QueryRow(`
		UPDATE financial_horizon_deductions
		SET name = $1, category = $2, amount = $3, due_day = $4, is_active = $5, budget_id = $6, updated_at = CURRENT_TIMESTAMP
		WHERE id = $7 AND user_id = $8
		RETURNING id, name, category, amount, due_day, is_active, budget_id
	`, in.Name, category, in.Amount, dueDay, isActive, budgetID, deductionID, user.ID).Scan(&item.ID, &item.Name, &item.Category, &item.Amount, &dueDay, &item.IsActive, &budgetID)

	if errors.Is(err, sql.ErrNoRows) {
		http.NotFound(w, r)
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	if dueDay.Valid {
		d := int(dueDay.Int64)
		item.DueDay = &d
	}
	if budgetID.Valid {
		bID := budgetID.Int64
		item.BudgetID = &bID
	}

	webutil.WriteJSON(w, http.StatusOK, item)
}

func (h *Handler) DeleteDeduction(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	deductionID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}

	result, err := h.DB.Exec(`DELETE FROM financial_horizon_deductions WHERE id = $1 AND user_id = $2`, deductionID, user.ID)
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
