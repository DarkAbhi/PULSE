package horizon

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"math"

	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/DarkAbhi/life-backend/internal/horizon/query"
)

type Service struct {
	db            *pgxpool.Pool
	budgets       *BudgetsHandler
	deductions    *DeductionsHandler
	categories    *CategoriesHandler
	transactions  *TransactionsHandler
	subscriptions *SubscriptionsHandler
}

func NewService(db *pgxpool.Pool) *Service {
	return &Service{
		db:            db,
		budgets:       NewBudgetsHandler(db, nil),
		deductions:    NewDeductionsHandler(db, nil),
		categories:    NewCategoriesHandler(db, nil),
		transactions:  NewTransactionsHandler(db, nil),
		subscriptions: NewSubscriptionsHandler(db, nil),
	}
}

func (s *Service) UpsertConfig(ctx context.Context, userID int64, baseAmount float64, currency string) error {
	return query.New(s.db).UpsertHorizonConfig(ctx, query.UpsertHorizonConfigParams{UserID: userID, BaseAmount: baseAmount, Currency: currency})
}

func (s *Service) Summary(ctx context.Context, userID int64) (*SummaryDTO, error) {

	if err := SeedDefaultCategories(ctx, s.db); err != nil {
		return nil, err
	}

	var baseAmount float64
	var currency string

	config, err := query.New(s.db).GetHorizonConfig(ctx, userID)
	if errors.Is(err, sql.ErrNoRows) {
		baseAmount = 0
		currency = "₹"
	} else if err != nil {
		return nil, fmt.Errorf("get horizon config: %w", err)
	} else {
		baseAmount, currency = config.BaseAmount, config.Currency
	}

	// Fetch Budgets
	budgetsList, totalBudgetsAllocated, err := s.budgets.FetchBudgets(userID)
	if err != nil {
		return nil, fmt.Errorf("fetch budgets: %w", err)
	}

	budgetsMap := make(map[int64]*BudgetDTO)
	for i := range budgetsList {
		budgetsMap[budgetsList[i].ID] = &budgetsList[i]
	}

	// Track budget usage from deductions & transactions
	budgetUsedMap := make(map[int64]float64)

	// Fetch Deductions
	deductionsList, totalDeductions, err := s.deductions.FetchDeductions(userID, budgetUsedMap)
	if err != nil {
		return nil, fmt.Errorf("fetch deductions: %w", err)
	}

	// Fetch Subscriptions
	subscriptionsList, totalSubscriptionBurn, err := s.subscriptions.FetchSubscriptions(userID)
	if err != nil {
		return nil, fmt.Errorf("fetch subscriptions: %w", err)
	}

	// Fetch Categories
	categoriesList, err := s.categories.FetchCategories(userID)
	if err != nil {
		return nil, fmt.Errorf("fetch categories: %w", err)
	}

	// Fetch Transactions
	transactionsList, totalTransactions, err := s.transactions.FetchTransactions(userID, 50)
	if err != nil {
		return nil, fmt.Errorf("fetch transactions: %w", err)
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

	return &SummaryDTO{
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
