package deductions

import (
	"context"
	"database/sql"
	"encoding/json"
	"errors"
	"net/http"
	"strings"

	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/DarkAbhi/life-backend/internal/db/sqlc"
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
	rows, err := sqlc.New(h.DB).ListDeductions(context.Background(), userID)
	if err != nil {
		return nil, 0, err
	}

	deductions := make([]DeductionDTO, 0)
	var totalDeductions float64

	for _, row := range rows {
		item := DeductionDTO{ID: row.ID, Name: row.Name, Category: row.Category, Amount: row.Amount, IsActive: row.IsActive}
		if row.DueDay.Valid {
			d := int(row.DueDay.Int16)
			item.DueDay = &d
		}
		if row.BudgetID.Valid {
			bID := row.BudgetID.Int64
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

	var dueDay sql.NullInt16
	if in.DueDay != nil {
		dueDay = sql.NullInt16{Int16: int16(*in.DueDay), Valid: true}
	}

	row, err := sqlc.New(h.DB).CreateDeduction(r.Context(), sqlc.CreateDeductionParams{UserID: user.ID, Name: in.Name, Category: category, Amount: in.Amount, DueDay: dueDay, IsActive: isActive, BudgetID: budgetID})

	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	item := DeductionDTO{ID: row.ID, Name: row.Name, Category: row.Category, Amount: row.Amount, IsActive: row.IsActive}
	if row.DueDay.Valid {
		d := int(row.DueDay.Int16)
		item.DueDay = &d
	}
	if row.BudgetID.Valid {
		bID := row.BudgetID.Int64
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

	var dueDay sql.NullInt16
	if in.DueDay != nil {
		dueDay = sql.NullInt16{Int16: int16(*in.DueDay), Valid: true}
	}

	row, err := sqlc.New(h.DB).UpdateDeduction(r.Context(), sqlc.UpdateDeductionParams{Name: in.Name, Category: category, Amount: in.Amount, DueDay: dueDay, IsActive: isActive, BudgetID: budgetID, ID: deductionID, UserID: user.ID})

	if errors.Is(err, sql.ErrNoRows) {
		http.NotFound(w, r)
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	item := DeductionDTO{ID: row.ID, Name: row.Name, Category: row.Category, Amount: row.Amount, IsActive: row.IsActive}
	if row.DueDay.Valid {
		d := int(row.DueDay.Int16)
		item.DueDay = &d
	}
	if row.BudgetID.Valid {
		bID := row.BudgetID.Int64
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

	deleted, err := sqlc.New(h.DB).DeleteDeduction(r.Context(), sqlc.DeleteDeductionParams{ID: deductionID, UserID: user.ID})
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
