package horizon

import (
	"database/sql"
	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/jackc/pgx/v5/pgtype"
	"github.com/jackc/pgx/v5/pgxpool"
	"net/http"
)

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

type SummaryDTO struct {
	BaseAmount            float64           `json:"base_amount"`
	Currency              string            `json:"currency"`
	TotalDeductions       float64           `json:"total_deductions"`
	TotalTransactions     float64           `json:"total_transactions"`
	RemainingAmount       float64           `json:"remaining_amount"`
	CommittedRatio        float64           `json:"committed_ratio"`
	TotalBudgetsAllocated float64           `json:"total_budgets_allocated"`
	TotalSubscriptionBurn float64           `json:"total_subscription_burn"`
	Budgets               []BudgetDTO       `json:"budgets"`
	Deductions            []DeductionDTO    `json:"deductions"`
	Categories            []CategoryDTO     `json:"categories"`
	Transactions          []TransactionDTO  `json:"transactions"`
	Subscriptions         []SubscriptionDTO `json:"subscriptions"`
	Projections           []ProjectionDTO   `json:"projections"`
}

type SessionLookup func(*http.Request) (auth.SessionUser, error)

type Handler struct {
	sessions      SessionLookup
	service       *Service
	DB            *pgxpool.Pool
	Budgets       *BudgetsHandler
	Deductions    *DeductionsHandler
	Categories    *CategoriesHandler
	Transactions  *TransactionsHandler
	Subscriptions *SubscriptionsHandler
}

func NewHandler(db *pgxpool.Pool, service *Service, lookup SessionLookup) *Handler {
	return &Handler{
		sessions:      lookup,
		service:       service,
		DB:            db,
		Budgets:       NewBudgetsHandler(db, lookup),
		Deductions:    NewDeductionsHandler(db, lookup),
		Categories:    NewCategoriesHandler(db, lookup),
		Transactions:  NewTransactionsHandler(db, lookup),
		Subscriptions: NewSubscriptionsHandler(db, lookup),
	}
}

func (h *Handler) sessionUser(r *http.Request) (auth.SessionUser, error) { return h.sessions(r) }

func pgTime(value sql.NullTime) pgtype.Timestamptz {
	return pgtype.Timestamptz{Time: value.Time, Valid: value.Valid}
}
func pgText(value sql.NullString) pgtype.Text {
	return pgtype.Text{String: value.String, Valid: value.Valid}
}
func sqlText(value pgtype.Text) sql.NullString {
	return sql.NullString{String: value.String, Valid: value.Valid}
}
