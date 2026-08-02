package transactions

import (
	"database/sql"
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type TransactionInput struct {
	Name            string  `json:"name"`
	Amount          float64 `json:"amount"`
	Type            *string `json:"type"`
	TransactionDate *string `json:"transaction_date"`
	CategoryID      *int64  `json:"category_id"`
	CategoryName    *string `json:"category_name"`
	BudgetID        *int64  `json:"budget_id"`
	SubscriptionID  *int64  `json:"subscription_id"`
	Notes           *string `json:"notes"`
}

type TransactionDTO struct {
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

type BulkTransactionsInput struct {
	Items []TransactionInput `json:"items"`
}

type Handler struct {
	DB *sql.DB
}

func NewHandler(db *sql.DB) *Handler {
	return &Handler{DB: db}
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

func (h *Handler) FetchTransactions(userID int64, limit int) ([]TransactionDTO, float64, error) {
	if limit <= 0 {
		limit = 50
	}
	query := `
		SELECT t.id, t.name, t.amount, t.type, t.transaction_date, t.category_id, COALESCE(c.name, t.category_name), t.budget_id, b.name, t.subscription_id, s.name, t.notes, t.created_at
		FROM financial_horizon_transactions t
		LEFT JOIN financial_horizon_categories c ON t.category_id = c.id
		LEFT JOIN financial_horizon_budgets b ON t.budget_id = b.id
		LEFT JOIN financial_horizon_subscriptions s ON t.subscription_id = s.id
		WHERE t.user_id = $1
		ORDER BY t.transaction_date DESC, t.id DESC
		LIMIT $2
	`
	rows, err := h.DB.Query(query, userID, limit)
	if err != nil {
		fallbackQuery := `
			SELECT t.id, t.name, t.amount, t.type, t.transaction_date, t.category_id, COALESCE(c.name, t.category_name), t.budget_id, b.name, NULL, NULL, t.notes, t.created_at
			FROM financial_horizon_transactions t
			LEFT JOIN financial_horizon_categories c ON t.category_id = c.id
			LEFT JOIN financial_horizon_budgets b ON t.budget_id = b.id
			WHERE t.user_id = $1
			ORDER BY t.transaction_date DESC, t.id DESC
			LIMIT $2
		`
		var fallbackErr error
		rows, fallbackErr = h.DB.Query(fallbackQuery, userID, limit)
		if fallbackErr != nil {
			return nil, 0, err
		}
	}
	defer rows.Close()

	transactions := make([]TransactionDTO, 0)
	var total float64
	for rows.Next() {
		var item TransactionDTO
		var catID, bID, sID sql.NullInt64
		var bName, sName, notes sql.NullString

		if err := rows.Scan(&item.ID, &item.Name, &item.Amount, &item.Type, &item.TransactionDate, &catID, &item.CategoryName, &bID, &bName, &sID, &sName, &notes, &item.CreatedAt); err != nil {
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
		total += item.Amount
		transactions = append(transactions, item)
	}
	return transactions, total, nil
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

	transactions, _, err := h.FetchTransactions(user.ID, limit)
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

	var subID sql.NullInt64
	if in.SubscriptionID != nil && *in.SubscriptionID > 0 {
		subID = sql.NullInt64{Int64: *in.SubscriptionID, Valid: true}
	}

	var notes sql.NullString
	if in.Notes != nil && strings.TrimSpace(*in.Notes) != "" {
		notes = sql.NullString{String: strings.TrimSpace(*in.Notes), Valid: true}
	}

	txType := parseTransactionType(in.Type)

	var item TransactionDTO
	var catID, bIDVal, subIDVal sql.NullInt64
	var bName, subName, notesVal sql.NullString

	err = h.DB.QueryRow(`
		INSERT INTO financial_horizon_transactions (user_id, name, amount, type, transaction_date, category_id, category_name, budget_id, subscription_id, notes)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
		RETURNING id, name, amount, type, transaction_date, category_id, category_name, budget_id, subscription_id, notes, created_at
	`, user.ID, in.Name, in.Amount, txType, txTime, categoryID, categoryName, budgetID, subID, notes).Scan(
		&item.ID, &item.Name, &item.Amount, &item.Type, &item.TransactionDate, &catID, &item.CategoryName, &bIDVal, &subIDVal, &notesVal, &item.CreatedAt,
	)

	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	if catID.Valid {
		id := catID.Int64
		item.CategoryID = &id
	}
	if bIDVal.Valid {
		id := bIDVal.Int64
		item.BudgetID = &id
		_ = h.DB.QueryRow(`SELECT name FROM financial_horizon_budgets WHERE id = $1 AND user_id = $2`, id, user.ID).Scan(&bName)
		if bName.Valid {
			item.BudgetName = &bName.String
		}
	}
	if subIDVal.Valid {
		id := subIDVal.Int64
		item.SubscriptionID = &id
		_ = h.DB.QueryRow(`SELECT name FROM financial_horizon_subscriptions WHERE id = $1 AND user_id = $2`, id, user.ID).Scan(&subName)
		if subName.Valid {
			item.SubscriptionName = &subName.String
		}
	}
	if notesVal.Valid {
		item.Notes = &notesVal.String
	}

	webutil.WriteJSON(w, http.StatusCreated, item)
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

		var subID sql.NullInt64
		if in.SubscriptionID != nil && *in.SubscriptionID > 0 {
			subID = sql.NullInt64{Int64: *in.SubscriptionID, Valid: true}
		}

		var notes sql.NullString
		if in.Notes != nil && strings.TrimSpace(*in.Notes) != "" {
			notes = sql.NullString{String: strings.TrimSpace(*in.Notes), Valid: true}
		}

		txType := parseTransactionType(in.Type)

		var item TransactionDTO
		var catID, bIDVal, subIDVal sql.NullInt64
		var bName, subName, notesVal sql.NullString

		err = tx.QueryRow(`
			INSERT INTO financial_horizon_transactions (user_id, name, amount, type, transaction_date, category_id, category_name, budget_id, subscription_id, notes)
			VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
			RETURNING id, name, amount, type, transaction_date, category_id, category_name, budget_id, subscription_id, notes, created_at
		`, user.ID, in.Name, in.Amount, txType, txTime, categoryID, categoryName, budgetID, subID, notes).Scan(
			&item.ID, &item.Name, &item.Amount, &item.Type, &item.TransactionDate, &catID, &item.CategoryName, &bIDVal, &subIDVal, &notesVal, &item.CreatedAt,
		)
		if err != nil {
			webutil.ServerError(w, err)
			return
		}

		if catID.Valid {
			id := catID.Int64
			item.CategoryID = &id
		}
		if bIDVal.Valid {
			id := bIDVal.Int64
			item.BudgetID = &id
			_ = tx.QueryRow(`SELECT name FROM financial_horizon_budgets WHERE id = $1 AND user_id = $2`, id, user.ID).Scan(&bName)
			if bName.Valid {
				item.BudgetName = &bName.String
			}
		}
		if subIDVal.Valid {
			id := subIDVal.Int64
			item.SubscriptionID = &id
			_ = tx.QueryRow(`SELECT name FROM financial_horizon_subscriptions WHERE id = $1 AND user_id = $2`, id, user.ID).Scan(&subName)
			if subName.Valid {
				item.SubscriptionName = &subName.String
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

	var subID sql.NullInt64
	if in.SubscriptionID != nil && *in.SubscriptionID > 0 {
		subID = sql.NullInt64{Int64: *in.SubscriptionID, Valid: true}
	}

	var notes sql.NullString
	if in.Notes != nil && strings.TrimSpace(*in.Notes) != "" {
		notes = sql.NullString{String: strings.TrimSpace(*in.Notes), Valid: true}
	}

	txType := parseTransactionType(in.Type)

	var item TransactionDTO
	var catID, bIDVal, subIDVal sql.NullInt64
	var bName, subName, notesVal sql.NullString

	err = h.DB.QueryRow(`
		UPDATE financial_horizon_transactions
		SET name = $1, amount = $2, type = $3, transaction_date = $4, category_id = $5, category_name = $6, budget_id = $7, subscription_id = $8, notes = $9, updated_at = CURRENT_TIMESTAMP
		WHERE id = $10 AND user_id = $11
		RETURNING id, name, amount, type, transaction_date, category_id, category_name, budget_id, subscription_id, notes, created_at
	`, in.Name, in.Amount, txType, txTime, categoryID, categoryName, budgetID, subID, notes, txID, user.ID).Scan(
		&item.ID, &item.Name, &item.Amount, &item.Type, &item.TransactionDate, &catID, &item.CategoryName, &bIDVal, &subIDVal, &notesVal, &item.CreatedAt,
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
	if bIDVal.Valid {
		id := bIDVal.Int64
		item.BudgetID = &id
		_ = h.DB.QueryRow(`SELECT name FROM financial_horizon_budgets WHERE id = $1 AND user_id = $2`, id, user.ID).Scan(&bName)
		if bName.Valid {
			item.BudgetName = &bName.String
		}
	}
	if subIDVal.Valid {
		id := subIDVal.Int64
		item.SubscriptionID = &id
		_ = h.DB.QueryRow(`SELECT name FROM financial_horizon_subscriptions WHERE id = $1 AND user_id = $2`, id, user.ID).Scan(&subName)
		if subName.Valid {
			item.SubscriptionName = &subName.String
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
