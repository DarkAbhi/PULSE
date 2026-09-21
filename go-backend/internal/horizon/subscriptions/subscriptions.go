package subscriptions

import (
	"database/sql"
	"encoding/json"
	"errors"
	"math"
	"net/http"
	"strings"
	"time"

	"github.com/DarkAbhi/life-backend/internal/auth"
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
	query := `
		SELECT s.id, s.name, s.amount, s.billing_cycle, s.billing_day, s.renewal_date, s.status,
		       s.category_id, c.name, s.budget_id, b.name, s.deduction_id, s.notes, s.created_at, s.updated_at,
		       COALESCE(COUNT(t.id), 0) AS linked_count,
		       COALESCE(SUM(t.amount), 0) AS total_spent
		FROM financial_horizon_subscriptions s
		LEFT JOIN financial_horizon_categories c ON s.category_id = c.id
		LEFT JOIN financial_horizon_budgets b ON s.budget_id = b.id
		LEFT JOIN financial_horizon_transactions t ON t.subscription_id = s.id
		WHERE s.user_id = $1
		GROUP BY s.id, c.name, b.name
		ORDER BY CASE WHEN s.status = 'active' THEN 1 WHEN s.status = 'paused' THEN 2 ELSE 3 END, s.created_at DESC, s.id DESC
	`
	rows, err := h.DB.Query(query, userID)
	if err != nil {
		// Return empty list safely if table or migration is missing
		return make([]SubscriptionDTO, 0), 0, nil
	}
	defer rows.Close()

	now := time.Now()
	subscriptions := make([]SubscriptionDTO, 0)
	var totalMonthlyBurn float64

	for rows.Next() {
		var s SubscriptionDTO
		var bDay sql.NullInt64
		var rDate sql.NullTime
		var catID, bID, dID sql.NullInt64
		var catName, bName, notes sql.NullString

		if err := rows.Scan(
			&s.ID, &s.Name, &s.Amount, &s.BillingCycle, &bDay, &rDate, &s.Status,
			&catID, &catName, &bID, &bName, &dID, &notes, &s.CreatedAt, &s.UpdatedAt,
			&s.LinkedTransactionCount, &s.TotalSpent,
		); err != nil {
			return nil, 0, err
		}

		if bDay.Valid {
			day := int(bDay.Int64)
			s.BillingDay = &day
		}
		if rDate.Valid {
			s.RenewalDate = &rDate.Time
		}

		if catID.Valid {
			id := catID.Int64
			s.CategoryID = &id
		}
		if catName.Valid {
			s.CategoryName = &catName.String
		}
		if bID.Valid {
			id := bID.Int64
			s.BudgetID = &id
		}
		if bName.Valid {
			s.BudgetName = &bName.String
		}
		if dID.Valid {
			id := dID.Int64
			s.DeductionID = &id
		}
		if notes.Valid {
			s.Notes = &notes.String
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

	var bDay sql.NullInt64
	if in.BillingDay != nil && *in.BillingDay >= 1 && *in.BillingDay <= 31 {
		bDay = sql.NullInt64{Int64: int64(*in.BillingDay), Valid: true}
	} else if rDate.Valid {
		bDay = sql.NullInt64{Int64: int64(rDate.Time.Day()), Valid: true}
	} else if cycle == "monthly" {
		bDay = sql.NullInt64{Int64: 1, Valid: true}
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

	var s SubscriptionDTO
	var bDayRet sql.NullInt64
	var rDateRet sql.NullTime
	var cID, bgID, dcID sql.NullInt64
	var cName, bName, notesVal sql.NullString

	err = h.DB.QueryRow(`
		INSERT INTO financial_horizon_subscriptions (user_id, name, amount, billing_cycle, billing_day, renewal_date, status, category_id, budget_id, deduction_id, notes)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
		RETURNING id, name, amount, billing_cycle, billing_day, renewal_date, status, category_id, budget_id, deduction_id, notes, created_at, updated_at
	`, user.ID, in.Name, in.Amount, cycle, bDay, rDate, status, catID, bID, dID, notes).Scan(
		&s.ID, &s.Name, &s.Amount, &s.BillingCycle, &bDayRet, &rDateRet, &s.Status, &cID, &bgID, &dcID, &notesVal, &s.CreatedAt, &s.UpdatedAt,
	)

	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	if bDayRet.Valid {
		day := int(bDayRet.Int64)
		s.BillingDay = &day
	}
	if rDateRet.Valid {
		s.RenewalDate = &rDateRet.Time
	}

	if cID.Valid {
		id := cID.Int64
		s.CategoryID = &id
		_ = h.DB.QueryRow(`SELECT name FROM financial_horizon_categories WHERE id = $1`, id).Scan(&cName)
		if cName.Valid {
			s.CategoryName = &cName.String
		}
	}
	if bgID.Valid {
		id := bgID.Int64
		s.BudgetID = &id
		_ = h.DB.QueryRow(`SELECT name FROM financial_horizon_budgets WHERE id = $1 AND user_id = $2`, id, user.ID).Scan(&bName)
		if bName.Valid {
			s.BudgetName = &bName.String
		}
	}
	if dcID.Valid {
		id := dcID.Int64
		s.DeductionID = &id
	}
	if notesVal.Valid {
		s.Notes = &notesVal.String
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

	var bDay sql.NullInt64
	if in.BillingDay != nil && *in.BillingDay >= 1 && *in.BillingDay <= 31 {
		bDay = sql.NullInt64{Int64: int64(*in.BillingDay), Valid: true}
	} else if rDate.Valid {
		bDay = sql.NullInt64{Int64: int64(rDate.Time.Day()), Valid: true}
	} else if cycle == "monthly" {
		bDay = sql.NullInt64{Int64: 1, Valid: true}
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

	var s SubscriptionDTO
	var bDayRet sql.NullInt64
	var rDateRet sql.NullTime
	var cID, bgID, dcID sql.NullInt64
	var cName, bName, notesVal sql.NullString

	err = h.DB.QueryRow(`
		UPDATE financial_horizon_subscriptions
		SET name = $1, amount = $2, billing_cycle = $3, billing_day = $4, renewal_date = $5, status = $6, category_id = $7, budget_id = $8, deduction_id = $9, notes = $10, updated_at = CURRENT_TIMESTAMP
		WHERE id = $11 AND user_id = $12
		RETURNING id, name, amount, billing_cycle, billing_day, renewal_date, status, category_id, budget_id, deduction_id, notes, created_at, updated_at
	`, in.Name, in.Amount, cycle, bDay, rDate, status, catID, bID, dID, notes, subID, user.ID).Scan(
		&s.ID, &s.Name, &s.Amount, &s.BillingCycle, &bDayRet, &rDateRet, &s.Status, &cID, &bgID, &dcID, &notesVal, &s.CreatedAt, &s.UpdatedAt,
	)

	if errors.Is(err, sql.ErrNoRows) {
		http.NotFound(w, r)
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	if bDayRet.Valid {
		day := int(bDayRet.Int64)
		s.BillingDay = &day
	}
	if rDateRet.Valid {
		s.RenewalDate = &rDateRet.Time
	}

	if cID.Valid {
		id := cID.Int64
		s.CategoryID = &id
		_ = h.DB.QueryRow(`SELECT name FROM financial_horizon_categories WHERE id = $1`, id).Scan(&cName)
		if cName.Valid {
			s.CategoryName = &cName.String
		}
	}
	if bgID.Valid {
		id := bgID.Int64
		s.BudgetID = &id
		_ = h.DB.QueryRow(`SELECT name FROM financial_horizon_budgets WHERE id = $1 AND user_id = $2`, id, user.ID).Scan(&bName)
		if bName.Valid {
			s.BudgetName = &bName.String
		}
	}
	if dcID.Valid {
		id := dcID.Int64
		s.DeductionID = &id
	}
	if notesVal.Valid {
		s.Notes = &notesVal.String
	}

	_ = h.DB.QueryRow(`SELECT COALESCE(COUNT(id), 0), COALESCE(SUM(amount), 0) FROM financial_horizon_transactions WHERE subscription_id = $1 AND user_id = $2`, subID, user.ID).Scan(&s.LinkedTransactionCount, &s.TotalSpent)

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

	result, err := h.DB.Exec(`DELETE FROM financial_horizon_subscriptions WHERE id = $1 AND user_id = $2`, subID, user.ID)
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

	query := `
		SELECT t.id, t.name, t.amount, t.type, t.transaction_date, t.category_id, COALESCE(c.name, t.category_name), t.budget_id, b.name, t.subscription_id, s.name, t.notes, t.created_at
		FROM financial_horizon_transactions t
		LEFT JOIN financial_horizon_categories c ON t.category_id = c.id
		LEFT JOIN financial_horizon_budgets b ON t.budget_id = b.id
		LEFT JOIN financial_horizon_subscriptions s ON t.subscription_id = s.id
		WHERE t.user_id = $1 AND t.subscription_id = $2
		ORDER BY t.transaction_date DESC, t.id DESC
	`
	rows, err := h.DB.Query(query, user.ID, subID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	defer rows.Close()

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

	for rows.Next() {
		var item struct {
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
		}
		var catID, bID, sID sql.NullInt64
		var bName, sName, notes sql.NullString

		if err := rows.Scan(&item.ID, &item.Name, &item.Amount, &item.Type, &item.TransactionDate, &catID, &item.CategoryName, &bID, &bName, &sID, &sName, &notes, &item.CreatedAt); err != nil {
			webutil.ServerError(w, err)
			return
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
		if sID.Valid {
			id := sID.Int64
			item.SubscriptionID = &id
		}
		if sName.Valid {
			item.SubscriptionName = &sName.String
		}
		if notes.Valid {
			item.Notes = &notes.String
		}
		transactions = append(transactions, item)
	}

	webutil.WriteJSON(w, http.StatusOK, transactions)
}
