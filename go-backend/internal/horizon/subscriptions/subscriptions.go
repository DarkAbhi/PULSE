package subscriptions

import (
	"context"
	"database/sql"
	"encoding/json"
	"errors"
	"math"
	"net/http"
	"strings"
	"time"

	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/DarkAbhi/life-backend/internal/db/sqlc"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type SubscriptionInput struct {
	Name         string  `json:"name"`
	Amount       float64 `json:"amount"`
	BillingCycle string  `json:"billing_cycle"` // 'monthly', 'yearly'
	BillingDay   *int    `json:"billing_day"`   // 1 to 31 (for monthly)
	RenewalDate  *string `json:"renewal_date"`  // ISO date string (for yearly)
	Status       *string `json:"status"`        // 'active', 'paused', 'cancelled'
	CategoryID   *int64  `json:"category_id"`
	BudgetID     *int64  `json:"budget_id"`
	DeductionID  *int64  `json:"deduction_id"`
	Notes        *string `json:"notes"`
}

type SubscriptionDTO struct {
	ID                      int64      `json:"id"`
	Name                    string     `json:"name"`
	Amount                  float64    `json:"amount"`
	BillingCycle            string     `json:"billing_cycle"`
	BillingDay              *int       `json:"billing_day,omitempty"`
	RenewalDate             *time.Time `json:"renewal_date,omitempty"`
	NextRenewalDate         time.Time  `json:"next_renewal_date"`
	MonthlyEquivalentAmount float64    `json:"monthly_equivalent_amount"`
	Status                  string     `json:"status"`
	CategoryID              *int64     `json:"category_id"`
	CategoryName            *string    `json:"category_name,omitempty"`
	BudgetID                *int64     `json:"budget_id"`
	BudgetName              *string    `json:"budget_name,omitempty"`
	DeductionID             *int64     `json:"deduction_id"`
	Notes                   *string    `json:"notes,omitempty"`
	LinkedTransactionCount  int        `json:"linked_transaction_count"`
	TotalSpent              float64    `json:"total_spent"`
	CreatedAt               time.Time  `json:"created_at"`
	UpdatedAt               time.Time  `json:"updated_at"`
}

type SubscriptionSummaryDTO struct {
	TotalMonthlyBurn         float64           `json:"total_monthly_burn"`
	TotalYearlyOutlay        float64           `json:"total_yearly_outlay"`
	ActiveSubscriptionsCount int               `json:"active_subscriptions_count"`
	Subscriptions            []SubscriptionDTO `json:"subscriptions"`
}

type Handler struct {
	DB *sql.DB
}

func NewHandler(db *sql.DB) *Handler {
	return &Handler{DB: db}
}

func CalculateMonthlyEquivalent(amount float64, cycle string) float64 {
	if strings.ToLower(cycle) == "yearly" {
		return math.Round((amount/12.0)*100) / 100
	}
	return amount
}

func daysInMonth(year int, month time.Month) int {
	return time.Date(year, month+1, 0, 0, 0, 0, 0, time.UTC).Day()
}

func CalculateNextRenewal(billingDay *int, renewalDate *time.Time, billingCycle string, refTime time.Time) time.Time {
	refYear, refMonth, refDay := refTime.Date()
	refDate := time.Date(refYear, refMonth, refDay, 0, 0, 0, 0, refTime.Location())

	if strings.ToLower(billingCycle) == "yearly" {
		renMonth, renDay := refMonth, refDay
		if renewalDate != nil && !renewalDate.IsZero() {
			renMonth = renewalDate.Month()
			renDay = renewalDate.Day()
		} else if billingDay != nil && *billingDay >= 1 && *billingDay <= 31 {
			renDay = *billingDay
		}

		dim := daysInMonth(refYear, renMonth)
		day := renDay
		if day > dim {
			day = dim
		}

		candidate := time.Date(refYear, renMonth, day, 0, 0, 0, 0, refTime.Location())
		if !candidate.Before(refDate) {
			return candidate
		}

		nextYear := refYear + 1
		dimNext := daysInMonth(nextYear, renMonth)
		dayNext := renDay
		if dayNext > dimNext {
			dayNext = dimNext
		}
		return time.Date(nextYear, renMonth, dayNext, 0, 0, 0, 0, refTime.Location())
	}

	// Monthly (default)
	dayVal := 1
	if billingDay != nil && *billingDay >= 1 && *billingDay <= 31 {
		dayVal = *billingDay
	} else if renewalDate != nil && !renewalDate.IsZero() {
		dayVal = renewalDate.Day()
	}

	dim := daysInMonth(refYear, refMonth)
	day := dayVal
	if day > dim {
		day = dim
	}

	candidate := time.Date(refYear, refMonth, day, 0, 0, 0, 0, refTime.Location())
	if !candidate.Before(refDate) {
		return candidate
	}

	nextMonth := refMonth + 1
	nextYear := refYear
	if nextMonth > 12 {
		nextMonth = 1
		nextYear++
	}
	dimNext := daysInMonth(nextYear, nextMonth)
	dayNext := dayVal
	if dayNext > dimNext {
		dayNext = dimNext
	}
	return time.Date(nextYear, nextMonth, dayNext, 0, 0, 0, 0, refTime.Location())
}

func (h *Handler) FetchSubscriptions(userID int64) ([]SubscriptionDTO, float64, error) {
	rows, err := sqlc.New(h.DB).ListSubscriptions(context.Background(), userID)
	if err != nil {
		// Return empty list safely if table or migration is missing
		return make([]SubscriptionDTO, 0), 0, nil
	}

	now := time.Now()
	subscriptions := make([]SubscriptionDTO, 0)
	var totalMonthlyBurn float64

	for _, row := range rows {
		s := SubscriptionDTO{ID: row.ID, Name: row.Name, Amount: row.Amount, BillingCycle: row.BillingCycle, Status: row.Status, CreatedAt: row.CreatedAt, UpdatedAt: row.UpdatedAt, LinkedTransactionCount: int(row.LinkedCount), TotalSpent: row.TotalSpent}
		if row.BillingDay.Valid {
			day := int(row.BillingDay.Int32)
			s.BillingDay = &day
		}
		if row.RenewalDate.Valid {
			s.RenewalDate = &row.RenewalDate.Time
		}

		if row.CategoryID.Valid {
			id := row.CategoryID.Int64
			s.CategoryID = &id
		}
		if row.CategoryName.Valid {
			s.CategoryName = &row.CategoryName.String
		}
		if row.BudgetID.Valid {
			id := row.BudgetID.Int64
			s.BudgetID = &id
		}
		if row.BudgetName.Valid {
			s.BudgetName = &row.BudgetName.String
		}
		if row.DeductionID.Valid {
			id := row.DeductionID.Int64
			s.DeductionID = &id
		}
		if row.Notes.Valid {
			s.Notes = &row.Notes.String
		}

		s.NextRenewalDate = CalculateNextRenewal(s.BillingDay, s.RenewalDate, s.BillingCycle, now)
		s.MonthlyEquivalentAmount = CalculateMonthlyEquivalent(s.Amount, s.BillingCycle)

		if s.Status == "active" {
			totalMonthlyBurn += s.MonthlyEquivalentAmount
		}

		subscriptions = append(subscriptions, s)
	}

	return subscriptions, totalMonthlyBurn, nil
}

func (h *Handler) ListSubscriptions(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	subscriptions, totalBurn, err := h.FetchSubscriptions(user.ID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	var yearlyOutlay float64
	var activeCount int
	for _, s := range subscriptions {
		if s.Status == "active" {
			activeCount++
			if strings.ToLower(s.BillingCycle) == "yearly" {
				yearlyOutlay += s.Amount
			} else {
				yearlyOutlay += s.Amount * 12
			}
		}
	}

	webutil.WriteJSON(w, http.StatusOK, SubscriptionSummaryDTO{
		TotalMonthlyBurn:         totalBurn,
		TotalYearlyOutlay:        yearlyOutlay,
		ActiveSubscriptionsCount: activeCount,
		Subscriptions:            subscriptions,
	})
}

func (h *Handler) CreateSubscription(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	var in SubscriptionInput
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<20)).Decode(&in); err != nil {
		webutil.BadRequest(w, "invalid JSON")
		return
	}

	in.Name = strings.TrimSpace(in.Name)
	if in.Name == "" || len([]rune(in.Name)) > 255 {
		webutil.BadRequest(w, "subscription name is required and must be under 255 characters")
		return
	}

	if in.Amount < 0 {
		webutil.BadRequest(w, "amount cannot be negative")
		return
	}

	cycle := strings.ToLower(strings.TrimSpace(in.BillingCycle))
	if cycle != "yearly" {
		cycle = "monthly"
	}

	var rDate sql.NullTime
	if in.RenewalDate != nil && strings.TrimSpace(*in.RenewalDate) != "" {
		if parsed, err := time.Parse(time.RFC3339, strings.TrimSpace(*in.RenewalDate)); err == nil {
			rDate = sql.NullTime{Time: parsed, Valid: true}
		} else if parsed, err := time.Parse("2006-01-02", strings.TrimSpace(*in.RenewalDate)); err == nil {
			rDate = sql.NullTime{Time: parsed, Valid: true}
		}
	}

	var bDay sql.NullInt32
	if in.BillingDay != nil && *in.BillingDay >= 1 && *in.BillingDay <= 31 {
		bDay = sql.NullInt32{Int32: int32(*in.BillingDay), Valid: true}
	} else if rDate.Valid {
		bDay = sql.NullInt32{Int32: int32(rDate.Time.Day()), Valid: true}
	} else if cycle == "monthly" {
		bDay = sql.NullInt32{Int32: 1, Valid: true}
	}

	status := "active"
	if in.Status != nil && strings.TrimSpace(*in.Status) != "" {
		s := strings.ToLower(strings.TrimSpace(*in.Status))
		if s == "paused" || s == "cancelled" {
			status = s
		}
	}

	var catID, bID, dID sql.NullInt64
	if in.CategoryID != nil && *in.CategoryID > 0 {
		catID = sql.NullInt64{Int64: *in.CategoryID, Valid: true}
	}
	if in.BudgetID != nil && *in.BudgetID > 0 {
		bID = sql.NullInt64{Int64: *in.BudgetID, Valid: true}
	}
	if in.DeductionID != nil && *in.DeductionID > 0 {
		dID = sql.NullInt64{Int64: *in.DeductionID, Valid: true}
	}

	var notes sql.NullString
	if in.Notes != nil && strings.TrimSpace(*in.Notes) != "" {
		notes = sql.NullString{String: strings.TrimSpace(*in.Notes), Valid: true}
	}

	q := sqlc.New(h.DB)
	row, err := q.CreateSubscription(r.Context(), sqlc.CreateSubscriptionParams{UserID: user.ID, Name: in.Name, Amount: in.Amount, BillingCycle: cycle, BillingDay: bDay, RenewalDate: rDate, Status: status, CategoryID: catID, BudgetID: bID, DeductionID: dID, Notes: notes})

	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	s := SubscriptionDTO{ID: row.ID, Name: row.Name, Amount: row.Amount, BillingCycle: row.BillingCycle, Status: row.Status, CreatedAt: row.CreatedAt, UpdatedAt: row.UpdatedAt}
	if row.BillingDay.Valid {
		day := int(row.BillingDay.Int32)
		s.BillingDay = &day
	}
	if row.RenewalDate.Valid {
		s.RenewalDate = &row.RenewalDate.Time
	}

	if row.CategoryID.Valid {
		id := row.CategoryID.Int64
		s.CategoryID = &id
		if name, err := q.GetCategoryName(r.Context(), id); err == nil {
			s.CategoryName = &name
		}
	}
	if row.BudgetID.Valid {
		id := row.BudgetID.Int64
		s.BudgetID = &id
		if name, err := q.GetBudgetName(r.Context(), sqlc.GetBudgetNameParams{ID: id, UserID: user.ID}); err == nil {
			s.BudgetName = &name
		}
	}
	if row.DeductionID.Valid {
		id := row.DeductionID.Int64
		s.DeductionID = &id
	}
	if row.Notes.Valid {
		s.Notes = &row.Notes.String
	}

	now := time.Now()
	s.NextRenewalDate = CalculateNextRenewal(s.BillingDay, s.RenewalDate, s.BillingCycle, now)
	s.MonthlyEquivalentAmount = CalculateMonthlyEquivalent(s.Amount, s.BillingCycle)
	s.LinkedTransactionCount = 0
	s.TotalSpent = 0

	webutil.WriteJSON(w, http.StatusCreated, s)
}

func (h *Handler) UpdateSubscription(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	subID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}

	var in SubscriptionInput
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<20)).Decode(&in); err != nil {
		webutil.BadRequest(w, "invalid JSON")
		return
	}

	in.Name = strings.TrimSpace(in.Name)
	if in.Name == "" || len([]rune(in.Name)) > 255 {
		webutil.BadRequest(w, "subscription name is required and must be under 255 characters")
		return
	}

	if in.Amount < 0 {
		webutil.BadRequest(w, "amount cannot be negative")
		return
	}

	cycle := strings.ToLower(strings.TrimSpace(in.BillingCycle))
	if cycle != "yearly" {
		cycle = "monthly"
	}

	var rDate sql.NullTime
	if in.RenewalDate != nil && strings.TrimSpace(*in.RenewalDate) != "" {
		if parsed, err := time.Parse(time.RFC3339, strings.TrimSpace(*in.RenewalDate)); err == nil {
			rDate = sql.NullTime{Time: parsed, Valid: true}
		} else if parsed, err := time.Parse("2006-01-02", strings.TrimSpace(*in.RenewalDate)); err == nil {
			rDate = sql.NullTime{Time: parsed, Valid: true}
		}
	}

	var bDay sql.NullInt32
	if in.BillingDay != nil && *in.BillingDay >= 1 && *in.BillingDay <= 31 {
		bDay = sql.NullInt32{Int32: int32(*in.BillingDay), Valid: true}
	} else if rDate.Valid {
		bDay = sql.NullInt32{Int32: int32(rDate.Time.Day()), Valid: true}
	} else if cycle == "monthly" {
		bDay = sql.NullInt32{Int32: 1, Valid: true}
	}

	status := "active"
	if in.Status != nil && strings.TrimSpace(*in.Status) != "" {
		s := strings.ToLower(strings.TrimSpace(*in.Status))
		if s == "paused" || s == "cancelled" {
			status = s
		}
	}

	var catID, bID, dID sql.NullInt64
	if in.CategoryID != nil && *in.CategoryID > 0 {
		catID = sql.NullInt64{Int64: *in.CategoryID, Valid: true}
	}
	if in.BudgetID != nil && *in.BudgetID > 0 {
		bID = sql.NullInt64{Int64: *in.BudgetID, Valid: true}
	}
	if in.DeductionID != nil && *in.DeductionID > 0 {
		dID = sql.NullInt64{Int64: *in.DeductionID, Valid: true}
	}

	var notes sql.NullString
	if in.Notes != nil && strings.TrimSpace(*in.Notes) != "" {
		notes = sql.NullString{String: strings.TrimSpace(*in.Notes), Valid: true}
	}

	q := sqlc.New(h.DB)
	row, err := q.UpdateSubscription(r.Context(), sqlc.UpdateSubscriptionParams{Name: in.Name, Amount: in.Amount, BillingCycle: cycle, BillingDay: bDay, RenewalDate: rDate, Status: status, CategoryID: catID, BudgetID: bID, DeductionID: dID, Notes: notes, ID: subID, UserID: user.ID})

	if errors.Is(err, sql.ErrNoRows) {
		http.NotFound(w, r)
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	s := SubscriptionDTO{ID: row.ID, Name: row.Name, Amount: row.Amount, BillingCycle: row.BillingCycle, Status: row.Status, CreatedAt: row.CreatedAt, UpdatedAt: row.UpdatedAt}
	if row.BillingDay.Valid {
		day := int(row.BillingDay.Int32)
		s.BillingDay = &day
	}
	if row.RenewalDate.Valid {
		s.RenewalDate = &row.RenewalDate.Time
	}

	if row.CategoryID.Valid {
		id := row.CategoryID.Int64
		s.CategoryID = &id
		if name, err := q.GetCategoryName(r.Context(), id); err == nil {
			s.CategoryName = &name
		}
	}
	if row.BudgetID.Valid {
		id := row.BudgetID.Int64
		s.BudgetID = &id
		if name, err := q.GetBudgetName(r.Context(), sqlc.GetBudgetNameParams{ID: id, UserID: user.ID}); err == nil {
			s.BudgetName = &name
		}
	}
	if row.DeductionID.Valid {
		id := row.DeductionID.Int64
		s.DeductionID = &id
	}
	if row.Notes.Valid {
		s.Notes = &row.Notes.String
	}

	if stats, err := q.GetSubscriptionTransactionStats(r.Context(), sqlc.GetSubscriptionTransactionStatsParams{SubscriptionID: sql.NullInt64{Int64: subID, Valid: true}, UserID: user.ID}); err == nil {
		s.LinkedTransactionCount, s.TotalSpent = int(stats.LinkedCount), stats.TotalSpent
	}

	now := time.Now()
	s.NextRenewalDate = CalculateNextRenewal(s.BillingDay, s.RenewalDate, s.BillingCycle, now)
	s.MonthlyEquivalentAmount = CalculateMonthlyEquivalent(s.Amount, s.BillingCycle)

	webutil.WriteJSON(w, http.StatusOK, s)
}

func (h *Handler) DeleteSubscription(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	subID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}

	deleted, err := sqlc.New(h.DB).DeleteSubscription(r.Context(), sqlc.DeleteSubscriptionParams{ID: subID, UserID: user.ID})
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

func (h *Handler) ListSubscriptionTransactions(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	subID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}

	rows, err := sqlc.New(h.DB).ListSubscriptionTransactions(r.Context(), sqlc.ListSubscriptionTransactionsParams{UserID: user.ID, SubscriptionID: sql.NullInt64{Int64: subID, Valid: true}})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	transactions := make([]struct {
		ID               int64     `json:"id"`
		Name             string    `json:"name"`
		Amount           float64   `json:"amount"`
		Type             string    `json:"type"`
		TransactionDate  time.Time `json:"transaction_date"`
		CategoryID       *int64    `json:"category_id"`
		CategoryName     string    `json:"category_name"`
		BudgetID         *int64    `json:"budget_id"`
		BudgetName       *string   `json:"budget_name,omitempty"`
		SubscriptionID   *int64    `json:"subscription_id,omitempty"`
		SubscriptionName *string   `json:"subscription_name,omitempty"`
		Notes            *string   `json:"notes,omitempty"`
		CreatedAt        time.Time `json:"created_at"`
	}, 0)

	for _, row := range rows {
		item := struct {
			ID               int64     `json:"id"`
			Name             string    `json:"name"`
			Amount           float64   `json:"amount"`
			Type             string    `json:"type"`
			TransactionDate  time.Time `json:"transaction_date"`
			CategoryID       *int64    `json:"category_id"`
			CategoryName     string    `json:"category_name"`
			BudgetID         *int64    `json:"budget_id"`
			BudgetName       *string   `json:"budget_name,omitempty"`
			SubscriptionID   *int64    `json:"subscription_id,omitempty"`
			SubscriptionName *string   `json:"subscription_name,omitempty"`
			Notes            *string   `json:"notes,omitempty"`
			CreatedAt        time.Time `json:"created_at"`
		}{ID: row.ID, Name: row.Name, Amount: row.Amount, Type: row.Type, TransactionDate: row.TransactionDate, CategoryName: row.CategoryName, CreatedAt: row.CreatedAt}
		if item.Type == "" {
			item.Type = "debit"
		}
		if row.CategoryID.Valid {
			id := row.CategoryID.Int64
			item.CategoryID = &id
		}
		if row.BudgetID.Valid {
			id := row.BudgetID.Int64
			item.BudgetID = &id
		}
		if row.BudgetName.Valid {
			item.BudgetName = &row.BudgetName.String
		}
		if row.SubscriptionID.Valid {
			id := row.SubscriptionID.Int64
			item.SubscriptionID = &id
		}
		if row.SubscriptionName.Valid {
			item.SubscriptionName = &row.SubscriptionName.String
		}
		if row.Notes.Valid {
			item.Notes = &row.Notes.String
		}
		transactions = append(transactions, item)
	}

	webutil.WriteJSON(w, http.StatusOK, transactions)
}
