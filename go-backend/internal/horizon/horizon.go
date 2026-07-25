package horizon

import (
	"database/sql"
	"encoding/json"
	"errors"
	"io"
	"math"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type ConfigInput struct {
	BaseAmount float64 `json:"base_amount"`
	Currency   *string `json:"currency"`
}

type ConfigDTO struct {
	BaseAmount float64 `json:"base_amount"`
	Currency   string  `json:"currency"`
}

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

type CategoryInput struct {
	Name  string  `json:"name"`
	Icon  *string `json:"icon"`
	Color *string `json:"color"`
}

type CategoryDTO struct {
	ID        int64  `json:"id"`
	Name      string `json:"name"`
	Icon      string `json:"icon"`
	Color     string `json:"color"`
	IsDefault bool   `json:"is_default"`
	UserID    *int64 `json:"user_id,omitempty"`
}

type TransactionInput struct {
	Name            string  `json:"name"`
	Amount          float64 `json:"amount"`
	Type            *string `json:"type"`
	TransactionDate *string `json:"transaction_date"`
	CategoryID      *int64  `json:"category_id"`
	CategoryName    *string `json:"category_name"`
	BudgetID        *int64  `json:"budget_id"`
	Notes           *string `json:"notes"`
}

type TransactionDTO struct {
	ID              int64     `json:"id"`
	Name            string    `json:"name"`
	Amount          float64   `json:"amount"`
	Type            string    `json:"type"`
	TransactionDate time.Time `json:"transaction_date"`
	CategoryID      *int64    `json:"category_id"`
	CategoryName    string    `json:"category_name"`
	BudgetID        *int64    `json:"budget_id"`
	BudgetName      *string   `json:"budget_name,omitempty"`
	Notes           *string   `json:"notes,omitempty"`
	CreatedAt       time.Time `json:"created_at"`
}

type ProjectionDTO struct {
	Months                int     `json:"months"`
	Label                 string  `json:"label"`
	CumulativeUncommitted float64 `json:"cumulative_uncommitted"`
}

type HorizonSummaryDTO struct {
	BaseAmount            float64          `json:"base_amount"`
	Currency              string           `json:"currency"`
	TotalDeductions       float64          `json:"total_deductions"`
	TotalTransactions     float64          `json:"total_transactions"`
	RemainingAmount       float64          `json:"remaining_amount"`
	CommittedRatio        float64          `json:"committed_ratio"`
	TotalBudgetsAllocated float64          `json:"total_budgets_allocated"`
	Budgets               []BudgetDTO      `json:"budgets"`
	Deductions            []DeductionDTO   `json:"deductions"`
	Categories            []CategoryDTO    `json:"categories"`
	Transactions          []TransactionDTO `json:"transactions"`
	Projections           []ProjectionDTO  `json:"projections"`
}

type Handler struct {
	DB *sql.DB
}

func NewHandler(db *sql.DB) *Handler {
	return &Handler{DB: db}
}

var validCategories = map[string]bool{
	"housing":      true,
	"investment":   true,
	"bill":         true,
	"subscription": true,
	"debt":         true,
	"other":        true,
}

func (h *Handler) GetHorizon(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	summary, err := h.fetchHorizonSummary(user.ID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	webutil.WriteJSON(w, http.StatusOK, summary)
}

func (h *Handler) fetchHorizonSummary(userID int64) (*HorizonSummaryDTO, error) {
	_ = SeedDefaultCategories(h.DB)

	var baseAmount float64
	var currency string

	err := h.DB.QueryRow(`SELECT base_amount, currency FROM financial_horizon_configs WHERE user_id = $1`, userID).Scan(&baseAmount, &currency)
	if errors.Is(err, sql.ErrNoRows) {
		baseAmount = 0
		currency = "₹"
	} else if err != nil {
		return nil, err
	}

	// Fetch Budgets
	budgetRows, err := h.DB.Query(`SELECT id, name, allocated_amount FROM financial_horizon_budgets WHERE user_id = $1 ORDER BY created_at ASC, id ASC`, userID)
	if err != nil {
		return nil, err
	}
	defer budgetRows.Close()

	budgetsMap := make(map[int64]*BudgetDTO)
	budgetsList := make([]BudgetDTO, 0)
	var totalBudgetsAllocated float64

	for budgetRows.Next() {
		var b BudgetDTO
		if err := budgetRows.Scan(&b.ID, &b.Name, &b.AllocatedAmount); err != nil {
			return nil, err
		}
		totalBudgetsAllocated += b.AllocatedAmount
		budgetsList = append(budgetsList, b)
	}

	// Fetch Deductions
	rows, err := h.DB.Query(`SELECT id, name, category, amount, due_day, is_active, budget_id FROM financial_horizon_deductions WHERE user_id = $1 ORDER BY is_active DESC, created_at DESC, id DESC`, userID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	budgetUsedMap := make(map[int64]float64)
	deductions := make([]DeductionDTO, 0)
	var totalDeductions float64

	for rows.Next() {
		var item DeductionDTO
		var dueDay sql.NullInt64
		var budgetID sql.NullInt64

		if err := rows.Scan(&item.ID, &item.Name, &item.Category, &item.Amount, &dueDay, &item.IsActive, &budgetID); err != nil {
			return nil, err
		}
		if dueDay.Valid {
			d := int(dueDay.Int64)
			item.DueDay = &d
		}
		if budgetID.Valid {
			bID := budgetID.Int64
			item.BudgetID = &bID
			if item.IsActive {
				budgetUsedMap[bID] += item.Amount
			}
		}

		if item.IsActive {
			totalDeductions += item.Amount
		}
		deductions = append(deductions, item)
	}

	// Fetch Categories
	categories, err := h.fetchCategories(userID)
	if err != nil {
		return nil, err
	}

	// Fetch Transactions
	transactions, totalTransactions, err := h.fetchTransactions(userID, 50)
	if err != nil {
		return nil, err
	}

	// Add transaction usage to budget map
	for _, t := range transactions {
		if t.BudgetID != nil {
			budgetUsedMap[*t.BudgetID] += t.Amount
		}
	}

	// Populate budget used, available, and percentage
	for i := range budgetsList {
		b := &budgetsList[i]
		b.UsedAmount = budgetUsedMap[b.ID]
		b.AvailableAmount = b.AllocatedAmount - b.UsedAmount
		if b.AllocatedAmount > 0 {
			b.UsagePercentage = math.Round((b.UsedAmount/b.AllocatedAmount)*10000) / 100
		}
		budgetsMap[b.ID] = b
	}

	remainingAmount := baseAmount - totalDeductions
	var committedRatio float64
	if baseAmount > 0 {
		committedRatio = math.Round((totalDeductions/baseAmount)*10000) / 100
	}

	projections := []ProjectionDTO{
		{Months: 3, Label: "3 Months", CumulativeUncommitted: math.Max(0, remainingAmount*3)},
		{Months: 6, Label: "6 Months", CumulativeUncommitted: math.Max(0, remainingAmount*6)},
		{Months: 12, Label: "1 Year", CumulativeUncommitted: math.Max(0, remainingAmount*12)},
	}

	return &HorizonSummaryDTO{
		BaseAmount:            baseAmount,
		Currency:              currency,
		TotalDeductions:       totalDeductions,
		TotalTransactions:     totalTransactions,
		RemainingAmount:       remainingAmount,
		CommittedRatio:        committedRatio,
		TotalBudgetsAllocated: totalBudgetsAllocated,
		Budgets:               budgetsList,
		Deductions:            deductions,
		Categories:            categories,
		Transactions:          transactions,
		Projections:           projections,
	}, nil
}

func (h *Handler) UpdateConfig(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	var in ConfigInput
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<20)).Decode(&in); err != nil {
		webutil.BadRequest(w, "invalid JSON")
		return
	}

	if in.BaseAmount < 0 {
		webutil.BadRequest(w, "base amount cannot be negative")
		return
	}

	currency := "₹"
	if in.Currency != nil && strings.TrimSpace(*in.Currency) != "" {
		currency = strings.TrimSpace(*in.Currency)
	}

	_, err = h.DB.Exec(`
		INSERT INTO financial_horizon_configs (user_id, base_amount, currency, updated_at)
		VALUES ($1, $2, $3, CURRENT_TIMESTAMP)
		ON CONFLICT (user_id) DO UPDATE
		SET base_amount = EXCLUDED.base_amount, currency = EXCLUDED.currency, updated_at = CURRENT_TIMESTAMP
	`, user.ID, in.BaseAmount, currency)

	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	summary, err := h.fetchHorizonSummary(user.ID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	webutil.WriteJSON(w, http.StatusOK, summary)
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
	if !validCategories[category] {
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
	if !validCategories[category] {
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

func (h *Handler) fetchCategories(userID int64) ([]CategoryDTO, error) {
	rows, err := h.DB.Query(`
		SELECT id, name, icon, color, is_default, user_id
		FROM financial_horizon_categories
		WHERE user_id IS NULL OR user_id = $1
		ORDER BY is_default DESC, name ASC
	`, userID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	categories := make([]CategoryDTO, 0)
	for rows.Next() {
		var c CategoryDTO
		var uid sql.NullInt64
		if err := rows.Scan(&c.ID, &c.Name, &c.Icon, &c.Color, &c.IsDefault, &uid); err != nil {
			return nil, err
		}
		if uid.Valid {
			u := uid.Int64
			c.UserID = &u
		}
		categories = append(categories, c)
	}
	return categories, nil
}

func parseTransactionType(raw *string) string {
	if raw == nil {
		return "debit"
	}
	val := strings.ToLower(strings.TrimSpace(*raw))
	if val == "credit" {
		return "credit"
	}
	return "debit"
}

func (h *Handler) fetchTransactions(userID int64, limit int) ([]TransactionDTO, float64, error) {
	if limit <= 0 {
		limit = 50
	}
	query := `
		SELECT t.id, t.name, t.amount, t.type, t.transaction_date, t.category_id, COALESCE(c.name, t.category_name), t.budget_id, b.name, t.notes, t.created_at
		FROM financial_horizon_transactions t
		LEFT JOIN financial_horizon_categories c ON t.category_id = c.id
		LEFT JOIN financial_horizon_budgets b ON t.budget_id = b.id
		WHERE t.user_id = $1
		ORDER BY t.transaction_date DESC, t.id DESC
		LIMIT $2
	`
	rows, err := h.DB.Query(query, userID, limit)
	if err != nil {
		return nil, 0, err
	}
	defer rows.Close()

	transactions := make([]TransactionDTO, 0)
	var total float64
	for rows.Next() {
		var item TransactionDTO
		var catID, bID sql.NullInt64
		var bName, notes sql.NullString

		if err := rows.Scan(&item.ID, &item.Name, &item.Amount, &item.Type, &item.TransactionDate, &catID, &item.CategoryName, &bID, &bName, &notes, &item.CreatedAt); err != nil {
			return nil, 0, err
		}
		if item.Type == "" {
			item.Type = "debit"
		}
		if catID.Valid {
			id := catID.Int64
			item.CategoryID = &id
		}
		if bID.Valid {
			id := bID.Int64
			item.BudgetID = &id
		}
		if bName.Valid {
			item.BudgetName = &bName.String
		}
		if notes.Valid {
			item.Notes = &notes.String
		}
		total += item.Amount
		transactions = append(transactions, item)
	}
	return transactions, total, nil
}

func (h *Handler) ListCategories(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	_ = SeedDefaultCategories(h.DB)
	categories, err := h.fetchCategories(user.ID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	webutil.WriteJSON(w, http.StatusOK, categories)
}

func (h *Handler) CreateCategory(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	var in CategoryInput
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<20)).Decode(&in); err != nil {
		webutil.BadRequest(w, "invalid JSON")
		return
	}

	in.Name = strings.TrimSpace(in.Name)
	if in.Name == "" || len([]rune(in.Name)) > 100 {
		webutil.BadRequest(w, "category name is required and must be under 100 characters")
		return
	}

	icon := "tag"
	if in.Icon != nil && strings.TrimSpace(*in.Icon) != "" {
		icon = strings.TrimSpace(*in.Icon)
	}

	color := "#64748b"
	if in.Color != nil && strings.TrimSpace(*in.Color) != "" {
		color = strings.TrimSpace(*in.Color)
	}

	var c CategoryDTO
	var userID sql.NullInt64
	err = h.DB.QueryRow(`
		INSERT INTO financial_horizon_categories (user_id, name, icon, color, is_default)
		VALUES ($1, $2, $3, $4, false)
		RETURNING id, name, icon, color, is_default, user_id
	`, user.ID, in.Name, icon, color).Scan(&c.ID, &c.Name, &c.Icon, &c.Color, &c.IsDefault, &userID)

	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	if userID.Valid {
		u := userID.Int64
		c.UserID = &u
	}

	webutil.WriteJSON(w, http.StatusCreated, c)
}

func (h *Handler) ListTransactions(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	limitStr := r.URL.Query().Get("limit")
	limit := 50
	if limitStr != "" {
		if parsed, err := strconv.Atoi(limitStr); err == nil && parsed > 0 {
			limit = parsed
		}
	}

	transactions, _, err := h.fetchTransactions(user.ID, limit)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	webutil.WriteJSON(w, http.StatusOK, transactions)
}

func (h *Handler) CreateTransaction(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	var in TransactionInput
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<20)).Decode(&in); err != nil {
		webutil.BadRequest(w, "invalid JSON")
		return
	}

	in.Name = strings.TrimSpace(in.Name)
	if in.Name == "" || len([]rune(in.Name)) > 255 {
		webutil.BadRequest(w, "transaction name is required and must be under 255 characters")
		return
	}

	if in.Amount <= 0 {
		webutil.BadRequest(w, "amount must be greater than zero")
		return
	}

	txTime := time.Now()
	if in.TransactionDate != nil && strings.TrimSpace(*in.TransactionDate) != "" {
		if parsed, err := time.Parse(time.RFC3339, strings.TrimSpace(*in.TransactionDate)); err == nil {
			txTime = parsed
		} else if parsed, err := time.Parse("2006-01-02T15:04", strings.TrimSpace(*in.TransactionDate)); err == nil {
			txTime = parsed
		} else if parsed, err := time.Parse("2006-01-02", strings.TrimSpace(*in.TransactionDate)); err == nil {
			txTime = parsed
		}
	}

	categoryName := "Other"
	var categoryID sql.NullInt64
	if in.CategoryID != nil && *in.CategoryID > 0 {
		categoryID = sql.NullInt64{Int64: *in.CategoryID, Valid: true}
		var cName string
		err := h.DB.QueryRow(`SELECT name FROM financial_horizon_categories WHERE id = $1 AND (user_id IS NULL OR user_id = $2)`, *in.CategoryID, user.ID).Scan(&cName)
		if err == nil {
			categoryName = cName
		}
	} else if in.CategoryName != nil && strings.TrimSpace(*in.CategoryName) != "" {
		categoryName = strings.TrimSpace(*in.CategoryName)
	}

	var budgetID sql.NullInt64
	if in.BudgetID != nil && *in.BudgetID > 0 {
		budgetID = sql.NullInt64{Int64: *in.BudgetID, Valid: true}
	}

	var notes sql.NullString
	if in.Notes != nil && strings.TrimSpace(*in.Notes) != "" {
		notes = sql.NullString{String: strings.TrimSpace(*in.Notes), Valid: true}
	}

	txType := parseTransactionType(in.Type)

	var item TransactionDTO
	var catID, bID sql.NullInt64
	var bName, notesVal sql.NullString

	err = h.DB.QueryRow(`
		INSERT INTO financial_horizon_transactions (user_id, name, amount, type, transaction_date, category_id, category_name, budget_id, notes)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
		RETURNING id, name, amount, type, transaction_date, category_id, category_name, budget_id, notes, created_at
	`, user.ID, in.Name, in.Amount, txType, txTime, categoryID, categoryName, budgetID, notes).Scan(
		&item.ID, &item.Name, &item.Amount, &item.Type, &item.TransactionDate, &catID, &item.CategoryName, &bID, &notesVal, &item.CreatedAt,
	)

	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	if catID.Valid {
		id := catID.Int64
		item.CategoryID = &id
	}
	if bID.Valid {
		id := bID.Int64
		item.BudgetID = &id
		_ = h.DB.QueryRow(`SELECT name FROM financial_horizon_budgets WHERE id = $1 AND user_id = $2`, id, user.ID).Scan(&bName)
		if bName.Valid {
			item.BudgetName = &bName.String
		}
	}
	if notesVal.Valid {
		item.Notes = &notesVal.String
	}

	webutil.WriteJSON(w, http.StatusCreated, item)
}

type BulkTransactionsInput struct {
	Items []TransactionInput `json:"items"`
}

func (h *Handler) BulkCreateTransactions(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	bodyBytes, err := io.ReadAll(http.MaxBytesReader(w, r.Body, 5<<20))
	if err != nil {
		webutil.BadRequest(w, "invalid request body")
		return
	}

	var inputs []TransactionInput
	if err := json.Unmarshal(bodyBytes, &inputs); err != nil {
		var container BulkTransactionsInput
		if err2 := json.Unmarshal(bodyBytes, &container); err2 == nil && len(container.Items) > 0 {
			inputs = container.Items
		} else {
			webutil.BadRequest(w, "invalid JSON array or items payload")
			return
		}
	}

	if len(inputs) == 0 {
		webutil.BadRequest(w, "at least one transaction is required")
		return
	}

	tx, err := h.DB.Begin()
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	defer tx.Rollback()

	createdItems := make([]TransactionDTO, 0, len(inputs))

	for _, in := range inputs {
		in.Name = strings.TrimSpace(in.Name)
		if in.Name == "" || len([]rune(in.Name)) > 255 {
			webutil.BadRequest(w, "transaction name is required and must be under 255 characters")
			return
		}
		if in.Amount <= 0 {
			webutil.BadRequest(w, "amount must be greater than zero")
			return
		}

		txTime := time.Now()
		if in.TransactionDate != nil && strings.TrimSpace(*in.TransactionDate) != "" {
			if parsed, err := time.Parse(time.RFC3339, strings.TrimSpace(*in.TransactionDate)); err == nil {
				txTime = parsed
			} else if parsed, err := time.Parse("2006-01-02T15:04", strings.TrimSpace(*in.TransactionDate)); err == nil {
				txTime = parsed
			} else if parsed, err := time.Parse("2006-01-02", strings.TrimSpace(*in.TransactionDate)); err == nil {
				txTime = parsed
			}
		}

		categoryName := "Other"
		var categoryID sql.NullInt64
		if in.CategoryID != nil && *in.CategoryID > 0 {
			categoryID = sql.NullInt64{Int64: *in.CategoryID, Valid: true}
			var cName string
			err := tx.QueryRow(`SELECT name FROM financial_horizon_categories WHERE id = $1 AND (user_id IS NULL OR user_id = $2)`, *in.CategoryID, user.ID).Scan(&cName)
			if err == nil {
				categoryName = cName
			}
		} else if in.CategoryName != nil && strings.TrimSpace(*in.CategoryName) != "" {
			categoryName = strings.TrimSpace(*in.CategoryName)
		}

		var budgetID sql.NullInt64
		if in.BudgetID != nil && *in.BudgetID > 0 {
			budgetID = sql.NullInt64{Int64: *in.BudgetID, Valid: true}
		}

		var notes sql.NullString
		if in.Notes != nil && strings.TrimSpace(*in.Notes) != "" {
			notes = sql.NullString{String: strings.TrimSpace(*in.Notes), Valid: true}
		}

		txType := parseTransactionType(in.Type)

		var item TransactionDTO
		var catID, bID sql.NullInt64
		var bName, notesVal sql.NullString

		err = tx.QueryRow(`
			INSERT INTO financial_horizon_transactions (user_id, name, amount, type, transaction_date, category_id, category_name, budget_id, notes)
			VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
			RETURNING id, name, amount, type, transaction_date, category_id, category_name, budget_id, notes, created_at
		`, user.ID, in.Name, in.Amount, txType, txTime, categoryID, categoryName, budgetID, notes).Scan(
			&item.ID, &item.Name, &item.Amount, &item.Type, &item.TransactionDate, &catID, &item.CategoryName, &bID, &notesVal, &item.CreatedAt,
		)
		if err != nil {
			webutil.ServerError(w, err)
			return
		}

		if catID.Valid {
			id := catID.Int64
			item.CategoryID = &id
		}
		if bID.Valid {
			id := bID.Int64
			item.BudgetID = &id
			_ = tx.QueryRow(`SELECT name FROM financial_horizon_budgets WHERE id = $1 AND user_id = $2`, id, user.ID).Scan(&bName)
			if bName.Valid {
				item.BudgetName = &bName.String
			}
		}
		if notesVal.Valid {
			item.Notes = &notesVal.String
		}

		createdItems = append(createdItems, item)
	}

	if err := tx.Commit(); err != nil {
		webutil.ServerError(w, err)
		return
	}

	webutil.WriteJSON(w, http.StatusCreated, createdItems)
}

func (h *Handler) UpdateTransaction(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	txID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}

	var in TransactionInput
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<20)).Decode(&in); err != nil {
		webutil.BadRequest(w, "invalid JSON")
		return
	}

	in.Name = strings.TrimSpace(in.Name)
	if in.Name == "" || len([]rune(in.Name)) > 255 {
		webutil.BadRequest(w, "transaction name is required and must be under 255 characters")
		return
	}

	if in.Amount <= 0 {
		webutil.BadRequest(w, "amount must be greater than zero")
		return
	}

	txTime := time.Now()
	if in.TransactionDate != nil && strings.TrimSpace(*in.TransactionDate) != "" {
		if parsed, err := time.Parse(time.RFC3339, strings.TrimSpace(*in.TransactionDate)); err == nil {
			txTime = parsed
		} else if parsed, err := time.Parse("2006-01-02T15:04", strings.TrimSpace(*in.TransactionDate)); err == nil {
			txTime = parsed
		} else if parsed, err := time.Parse("2006-01-02", strings.TrimSpace(*in.TransactionDate)); err == nil {
			txTime = parsed
		}
	}

	categoryName := "Other"
	var categoryID sql.NullInt64
	if in.CategoryID != nil && *in.CategoryID > 0 {
		categoryID = sql.NullInt64{Int64: *in.CategoryID, Valid: true}
		var cName string
		err := h.DB.QueryRow(`SELECT name FROM financial_horizon_categories WHERE id = $1 AND (user_id IS NULL OR user_id = $2)`, *in.CategoryID, user.ID).Scan(&cName)
		if err == nil {
			categoryName = cName
		}
	} else if in.CategoryName != nil && strings.TrimSpace(*in.CategoryName) != "" {
		categoryName = strings.TrimSpace(*in.CategoryName)
	}

	var budgetID sql.NullInt64
	if in.BudgetID != nil && *in.BudgetID > 0 {
		budgetID = sql.NullInt64{Int64: *in.BudgetID, Valid: true}
	}

	var notes sql.NullString
	if in.Notes != nil && strings.TrimSpace(*in.Notes) != "" {
		notes = sql.NullString{String: strings.TrimSpace(*in.Notes), Valid: true}
	}

	txType := parseTransactionType(in.Type)

	var item TransactionDTO
	var catID, bID sql.NullInt64
	var bName, notesVal sql.NullString

	err = h.DB.QueryRow(`
		UPDATE financial_horizon_transactions
		SET name = $1, amount = $2, type = $3, transaction_date = $4, category_id = $5, category_name = $6, budget_id = $7, notes = $8, updated_at = CURRENT_TIMESTAMP
		WHERE id = $9 AND user_id = $10
		RETURNING id, name, amount, type, transaction_date, category_id, category_name, budget_id, notes, created_at
	`, in.Name, in.Amount, txType, txTime, categoryID, categoryName, budgetID, notes, txID, user.ID).Scan(
		&item.ID, &item.Name, &item.Amount, &item.Type, &item.TransactionDate, &catID, &item.CategoryName, &bID, &notesVal, &item.CreatedAt,
	)

	if errors.Is(err, sql.ErrNoRows) {
		http.NotFound(w, r)
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	if catID.Valid {
		id := catID.Int64
		item.CategoryID = &id
	}
	if bID.Valid {
		id := bID.Int64
		item.BudgetID = &id
		_ = h.DB.QueryRow(`SELECT name FROM financial_horizon_budgets WHERE id = $1 AND user_id = $2`, id, user.ID).Scan(&bName)
		if bName.Valid {
			item.BudgetName = &bName.String
		}
	}
	if notesVal.Valid {
		item.Notes = &notesVal.String
	}

	webutil.WriteJSON(w, http.StatusOK, item)
}

func (h *Handler) DeleteTransaction(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	txID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}

	result, err := h.DB.Exec(`DELETE FROM financial_horizon_transactions WHERE id = $1 AND user_id = $2`, txID, user.ID)
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

func SeedDefaultCategories(db *sql.DB) error {
	var count int
	err := db.QueryRow(`SELECT COUNT(*) FROM financial_horizon_categories WHERE is_default = true`).Scan(&count)
	if err != nil {
		return err
	}
	if count > 0 {
		return nil
	}

	defaultCategories := []struct {
		name  string
		icon  string
		color string
	}{
		{"Food & Dining", "utensils", "#f97316"},
		{"Bills & Utilities", "receipt", "#ef4444"},
		{"Housing", "home", "#8b5cf6"},
		{"Transportation", "car", "#3b82f6"},
		{"Shopping", "shopping-bag", "#ec4899"},
		{"Entertainment", "film", "#14b8a6"},
		{"Health & Fitness", "heart-pulse", "#06b6d4"},
		{"Investments & Savings", "trending-up", "#22c55e"},
		{"Subscriptions", "credit-card", "#6366f1"},
		{"Other", "tag", "#64748b"},
	}

	for _, c := range defaultCategories {
		_, _ = db.Exec(`
			INSERT INTO financial_horizon_categories (name, icon, color, is_default)
			VALUES ($1, $2, $3, true)
			ON CONFLICT DO NOTHING
		`, c.name, c.icon, c.color)
	}
	return nil
}
