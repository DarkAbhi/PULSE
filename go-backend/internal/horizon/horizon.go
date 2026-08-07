package horizon

import (
	"database/sql"
	"encoding/json"
	"errors"
	"math"
	"net/http"
	"strings"

	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/DarkAbhi/life-backend/internal/horizon/budgets"
	"github.com/DarkAbhi/life-backend/internal/horizon/categories"
	"github.com/DarkAbhi/life-backend/internal/horizon/deductions"
	"github.com/DarkAbhi/life-backend/internal/horizon/subscriptions"
	"github.com/DarkAbhi/life-backend/internal/horizon/transactions"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

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
	if h.Budgets == nil {
		h.Budgets = budgets.NewHandler(h.DB)
	}
	if h.Deductions == nil {
		h.Deductions = deductions.NewHandler(h.DB)
	}
	if h.Categories == nil {
		h.Categories = categories.NewHandler(h.DB)
	}
	if h.Transactions == nil {
		h.Transactions = transactions.NewHandler(h.DB)
	}
	if h.Subscriptions == nil {
		h.Subscriptions = subscriptions.NewHandler(h.DB)
	}

	_ = categories.SeedDefaultCategories(h.DB)

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
	budgetsList, totalBudgetsAllocated, err := h.Budgets.FetchBudgets(userID)
	if err != nil {
		return nil, err
	}

	budgetsMap := make(map[int64]*budgets.BudgetDTO)
	for i := range budgetsList {
		budgetsMap[budgetsList[i].ID] = &budgetsList[i]
	}

	// Track budget usage from deductions & transactions
	budgetUsedMap := make(map[int64]float64)

	// Fetch Deductions
	deductionsList, totalDeductions, err := h.Deductions.FetchDeductions(userID, budgetUsedMap)
	if err != nil {
		return nil, err
	}

	// Fetch Subscriptions
	subscriptionsList, totalSubscriptionBurn, err := h.Subscriptions.FetchSubscriptions(userID)
	if err != nil {
		return nil, err
	}

	// Fetch Categories
	categoriesList, err := h.Categories.FetchCategories(userID)
	if err != nil {
		return nil, err
	}

	// Fetch Transactions
	transactionsList, totalTransactions, err := h.Transactions.FetchTransactions(userID, 50)
	if err != nil {
		return nil, err
	}

	// Add transaction usage to budget map
	for _, t := range transactionsList {
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
	}

	totalFixedObligations := totalDeductions + totalSubscriptionBurn
	remainingAmount := baseAmount - totalFixedObligations
	var committedRatio float64
	if baseAmount > 0 {
		committedRatio = math.Round((totalFixedObligations/baseAmount)*10000) / 100
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
		TotalSubscriptionBurn: totalSubscriptionBurn,
		Budgets:               budgetsList,
		Deductions:            deductionsList,
		Categories:            categoriesList,
		Transactions:          transactionsList,
		Subscriptions:         subscriptionsList,
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

// Delegate HTTP handlers to sub-package handlers safely

func (h *Handler) CreateBudget(w http.ResponseWriter, r *http.Request) {
	if h.Budgets == nil {
		h.Budgets = budgets.NewHandler(h.DB)
	}
	h.Budgets.CreateBudget(w, r)
}

func (h *Handler) UpdateBudget(w http.ResponseWriter, r *http.Request) {
	if h.Budgets == nil {
		h.Budgets = budgets.NewHandler(h.DB)
	}
	h.Budgets.UpdateBudget(w, r)
}

func (h *Handler) DeleteBudget(w http.ResponseWriter, r *http.Request) {
	if h.Budgets == nil {
		h.Budgets = budgets.NewHandler(h.DB)
	}
	h.Budgets.DeleteBudget(w, r)
}

func (h *Handler) CreateDeduction(w http.ResponseWriter, r *http.Request) {
	if h.Deductions == nil {
		h.Deductions = deductions.NewHandler(h.DB)
	}
	h.Deductions.CreateDeduction(w, r)
}

func (h *Handler) UpdateDeduction(w http.ResponseWriter, r *http.Request) {
	if h.Deductions == nil {
		h.Deductions = deductions.NewHandler(h.DB)
	}
	h.Deductions.UpdateDeduction(w, r)
}

func (h *Handler) DeleteDeduction(w http.ResponseWriter, r *http.Request) {
	if h.Deductions == nil {
		h.Deductions = deductions.NewHandler(h.DB)
	}
	h.Deductions.DeleteDeduction(w, r)
}

func (h *Handler) ListCategories(w http.ResponseWriter, r *http.Request) {
	if h.Categories == nil {
		h.Categories = categories.NewHandler(h.DB)
	}
	h.Categories.ListCategories(w, r)
}

func (h *Handler) CreateCategory(w http.ResponseWriter, r *http.Request) {
	if h.Categories == nil {
		h.Categories = categories.NewHandler(h.DB)
	}
	h.Categories.CreateCategory(w, r)
}

func (h *Handler) ListTransactions(w http.ResponseWriter, r *http.Request) {
	if h.Transactions == nil {
		h.Transactions = transactions.NewHandler(h.DB)
	}
	h.Transactions.ListTransactions(w, r)
}

func (h *Handler) CreateTransaction(w http.ResponseWriter, r *http.Request) {
	if h.Transactions == nil {
		h.Transactions = transactions.NewHandler(h.DB)
	}
	h.Transactions.CreateTransaction(w, r)
}

func (h *Handler) BulkCreateTransactions(w http.ResponseWriter, r *http.Request) {
	if h.Transactions == nil {
		h.Transactions = transactions.NewHandler(h.DB)
	}
	h.Transactions.BulkCreateTransactions(w, r)
}

func (h *Handler) UpdateTransaction(w http.ResponseWriter, r *http.Request) {
	if h.Transactions == nil {
		h.Transactions = transactions.NewHandler(h.DB)
	}
	h.Transactions.UpdateTransaction(w, r)
}

func (h *Handler) DeleteTransaction(w http.ResponseWriter, r *http.Request) {
	if h.Transactions == nil {
		h.Transactions = transactions.NewHandler(h.DB)
	}
	h.Transactions.DeleteTransaction(w, r)
}

func (h *Handler) ListSubscriptions(w http.ResponseWriter, r *http.Request) {
	if h.Subscriptions == nil {
		h.Subscriptions = subscriptions.NewHandler(h.DB)
	}
	h.Subscriptions.ListSubscriptions(w, r)
}

func (h *Handler) CreateSubscription(w http.ResponseWriter, r *http.Request) {
	if h.Subscriptions == nil {
		h.Subscriptions = subscriptions.NewHandler(h.DB)
	}
	h.Subscriptions.CreateSubscription(w, r)
}

func (h *Handler) UpdateSubscription(w http.ResponseWriter, r *http.Request) {
	if h.Subscriptions == nil {
		h.Subscriptions = subscriptions.NewHandler(h.DB)
	}
	h.Subscriptions.UpdateSubscription(w, r)
}

func (h *Handler) DeleteSubscription(w http.ResponseWriter, r *http.Request) {
	if h.Subscriptions == nil {
		h.Subscriptions = subscriptions.NewHandler(h.DB)
	}
	h.Subscriptions.DeleteSubscription(w, r)
}

func (h *Handler) ListSubscriptionTransactions(w http.ResponseWriter, r *http.Request) {
	if h.Subscriptions == nil {
		h.Subscriptions = subscriptions.NewHandler(h.DB)
	}
	h.Subscriptions.ListSubscriptionTransactions(w, r)
}
