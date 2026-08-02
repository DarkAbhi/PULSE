package horizon

import (
	"database/sql"

	"github.com/DarkAbhi/life-backend/internal/horizon/budgets"
	"github.com/DarkAbhi/life-backend/internal/horizon/categories"
	"github.com/DarkAbhi/life-backend/internal/horizon/deductions"
	"github.com/DarkAbhi/life-backend/internal/horizon/subscriptions"
	"github.com/DarkAbhi/life-backend/internal/horizon/transactions"
)

// Type Aliases for Sub-Package Domain DTOs & Inputs
type BudgetInput = budgets.BudgetInput
type BudgetDTO = budgets.BudgetDTO
type DeductionInput = deductions.DeductionInput
type DeductionDTO = deductions.DeductionDTO
type CategoryInput = categories.CategoryInput
type CategoryDTO = categories.CategoryDTO
type TransactionInput = transactions.TransactionInput
type TransactionDTO = transactions.TransactionDTO
type SubscriptionInput = subscriptions.SubscriptionInput
type SubscriptionDTO = subscriptions.SubscriptionDTO
type SubscriptionSummaryDTO = subscriptions.SubscriptionSummaryDTO

type ConfigInput struct {
	BaseAmount float64 `json:"base_amount"`
	Currency   *string `json:"currency"`
}

type ConfigDTO struct {
	BaseAmount float64 `json:"base_amount"`
	Currency   string  `json:"currency"`
}

type ProjectionDTO struct {
	Months                int     `json:"months"`
	Label                 string  `json:"label"`
	CumulativeUncommitted float64 `json:"cumulative_uncommitted"`
}

type HorizonSummaryDTO struct {
	BaseAmount            float64                 `json:"base_amount"`
	Currency              string                  `json:"currency"`
	TotalDeductions       float64                 `json:"total_deductions"`
	TotalTransactions     float64                 `json:"total_transactions"`
	RemainingAmount       float64                 `json:"remaining_amount"`
	CommittedRatio        float64                 `json:"committed_ratio"`
	TotalBudgetsAllocated float64                 `json:"total_budgets_allocated"`
	TotalSubscriptionBurn float64                 `json:"total_subscription_burn"`
	Budgets               []BudgetDTO             `json:"budgets"`
	Deductions            []DeductionDTO          `json:"deductions"`
	Categories            []CategoryDTO           `json:"categories"`
	Transactions          []TransactionDTO        `json:"transactions"`
	Subscriptions         []SubscriptionDTO       `json:"subscriptions"`
	Projections           []ProjectionDTO         `json:"projections"`
}

type Handler struct {
	DB            *sql.DB
	Budgets       *budgets.Handler
	Deductions    *deductions.Handler
	Categories    *categories.Handler
	Transactions  *transactions.Handler
	Subscriptions *subscriptions.Handler
}

func NewHandler(db *sql.DB) *Handler {
	return &Handler{
		DB:            db,
		Budgets:       budgets.NewHandler(db),
		Deductions:    deductions.NewHandler(db),
		Categories:    categories.NewHandler(db),
		Transactions:  transactions.NewHandler(db),
		Subscriptions: subscriptions.NewHandler(db),
	}
}
