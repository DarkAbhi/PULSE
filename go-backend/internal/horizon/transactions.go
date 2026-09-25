package horizon

import (
	"context"
	"database/sql"
	"encoding/json"
	"errors"
	"github.com/DarkAbhi/life-backend/internal/auth"
	"io"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/DarkAbhi/life-backend/internal/horizon/query"
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

// PaginatedTransactionsDTO is the response returned by the transaction list
// endpoint. Keeping the pagination details beside the records lets clients
// render numbered page controls without loading the complete history.
type PaginatedTransactionsDTO struct {
	Transactions []TransactionDTO `json:"transactions"`
	Page         int              `json:"page"`
	PageSize     int              `json:"page_size"`
	Total        int              `json:"total"`
	TotalPages   int              `json:"total_pages"`
}

type TransactionsHandler struct {
	sessions SessionLookup
	DB       *sql.DB
}

func NewTransactionsHandler(db *sql.DB, sessions SessionLookup) *TransactionsHandler {
	return &TransactionsHandler{DB: db, sessions: sessions}
}
func (h *TransactionsHandler) sessionUser(r *http.Request) (auth.SessionUser, error) {
	return h.sessions(r)
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

func (h *TransactionsHandler) FetchTransactions(userID int64, limit int) ([]TransactionDTO, float64, error) {
	if limit <= 0 {
		limit = 50
	}
	rows, err := query.New(h.DB).ListRecentTransactions(context.Background(), query.ListRecentTransactionsParams{UserID: userID, Limit: int32(limit)})
	if err != nil {
		legacy, legacyErr := query.New(h.DB).ListRecentTransactionsLegacy(context.Background(), query.ListRecentTransactionsLegacyParams{UserID: userID, Limit: int32(limit)})
		if legacyErr != nil {
			return nil, 0, err
		}
		transactions := make([]TransactionDTO, 0, len(legacy))
		var total float64
		for _, row := range legacy {
			item := transactionDTO(row.ID, row.Name, row.Amount, row.Type, row.TransactionDate, row.CategoryID, row.CategoryName, row.BudgetID, row.BudgetName, row.SubscriptionID, row.SubscriptionName, row.Notes, row.CreatedAt)
			total += item.Amount
			transactions = append(transactions, item)
		}
		return transactions, total, nil
	}

	transactions := make([]TransactionDTO, 0)
	var total float64
	for _, row := range rows {
		item := transactionDTO(row.ID, row.Name, row.Amount, row.Type, row.TransactionDate, row.CategoryID, row.CategoryName, row.BudgetID, row.BudgetName, row.SubscriptionID, row.SubscriptionName, row.Notes, row.CreatedAt)
		total += item.Amount
		transactions = append(transactions, item)
	}
	return transactions, total, nil
}

func transactionDTO(id int64, name string, amount float64, txType string, date time.Time, categoryID sql.NullInt64, categoryName string, budgetID sql.NullInt64, budgetName sql.NullString, subscriptionID sql.NullInt64, subscriptionName, notes sql.NullString, created time.Time) TransactionDTO {
	item := TransactionDTO{ID: id, Name: name, Amount: amount, Type: txType, TransactionDate: date, CategoryName: categoryName, CreatedAt: created}
	if item.Type == "" {
		item.Type = "debit"
	}
	if categoryID.Valid {
		item.CategoryID = &categoryID.Int64
	}
	if budgetID.Valid {
		item.BudgetID = &budgetID.Int64
	}
	if budgetName.Valid {
		item.BudgetName = &budgetName.String
	}
	if subscriptionID.Valid {
		item.SubscriptionID = &subscriptionID.Int64
	}
	if subscriptionName.Valid {
		item.SubscriptionName = &subscriptionName.String
	}
	if notes.Valid {
		item.Notes = &notes.String
	}
	return item
}

func enrichTransactionNames(ctx context.Context, q *query.Queries, userID int64, item *TransactionDTO) {
	if item.BudgetID != nil {
		if name, err := q.GetBudgetName(ctx, query.GetBudgetNameParams{ID: *item.BudgetID, UserID: userID}); err == nil {
			item.BudgetName = &name
		}
	}
	if item.SubscriptionID != nil {
		if name, err := q.GetSubscriptionName(ctx, query.GetSubscriptionNameParams{ID: *item.SubscriptionID, UserID: userID}); err == nil {
			item.SubscriptionName = &name
		}
	}
}

func (h *TransactionsHandler) ListTransactions(w http.ResponseWriter, r *http.Request) {
	user, err := h.sessionUser(r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	page := 1
	if rawPage := r.URL.Query().Get("page"); rawPage != "" {
		if parsed, err := strconv.Atoi(rawPage); err == nil && parsed > 0 {
			page = parsed
		}
	}

	pageSize := 10
	if rawPageSize := r.URL.Query().Get("page_size"); rawPageSize != "" {
		if parsed, err := strconv.Atoi(rawPageSize); err == nil && parsed > 0 {
			pageSize = parsed
		}
	}
	// Avoid allowing a caller to turn a paginated endpoint back into a full
	// table scan and response.
	if pageSize > 100 {
		pageSize = 100
	}

	// Optional filters
	txType := strings.ToLower(strings.TrimSpace(r.URL.Query().Get("type")))
	if txType != "debit" && txType != "credit" {
		txType = ""
	}
	search := strings.TrimSpace(r.URL.Query().Get("search"))

	searchPattern := ""
	if search != "" {
		searchPattern = "%" + strings.ToLower(search) + "%"
	}

	count, err := query.New(h.DB).CountFilteredTransactions(r.Context(), query.CountFilteredTransactionsParams{UserID: user.ID, Column2: txType, Column3: searchPattern})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	total := int(count)

	totalPages := (total + pageSize - 1) / pageSize
	if totalPages > 0 && page > totalPages {
		page = totalPages
	}
	if totalPages == 0 {
		page = 1
	}
	offset := (page - 1) * pageSize

	transactions, err := h.fetchTransactionsPage(user.ID, pageSize, offset, txType, search)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	webutil.WriteJSON(w, http.StatusOK, PaginatedTransactionsDTO{
		Transactions: transactions,
		Page:         page,
		PageSize:     pageSize,
		Total:        total,
		TotalPages:   totalPages,
	})
}

func (h *TransactionsHandler) fetchTransactionsPage(userID int64, limit, offset int, txType, search string) ([]TransactionDTO, error) {
	searchPattern := ""
	if search != "" {
		searchPattern = "%" + strings.ToLower(search) + "%"
	}
	rows, err := query.New(h.DB).ListFilteredTransactions(context.Background(), query.ListFilteredTransactionsParams{UserID: userID, Column2: txType, Column3: searchPattern, Limit: int32(limit), Offset: int32(offset)})
	if err != nil {
		legacy, legacyErr := query.New(h.DB).ListFilteredTransactionsLegacy(context.Background(), query.ListFilteredTransactionsLegacyParams{UserID: userID, Column2: txType, Column3: searchPattern, Limit: int32(limit), Offset: int32(offset)})
		if legacyErr != nil {
			return nil, err
		}
		items := make([]TransactionDTO, 0, len(legacy))
		for _, row := range legacy {
			items = append(items, transactionDTO(row.ID, row.Name, row.Amount, row.Type, row.TransactionDate, row.CategoryID, row.CategoryName, row.BudgetID, row.BudgetName, row.SubscriptionID, row.SubscriptionName, row.Notes, row.CreatedAt))
		}
		return items, nil
	}

	items := make([]TransactionDTO, 0)
	for _, row := range rows {
		items = append(items, transactionDTO(row.ID, row.Name, row.Amount, row.Type, row.TransactionDate, row.CategoryID, row.CategoryName, row.BudgetID, row.BudgetName, row.SubscriptionID, row.SubscriptionName, row.Notes, row.CreatedAt))
	}
	return items, nil
}

func (h *TransactionsHandler) CreateTransaction(w http.ResponseWriter, r *http.Request) {
	user, err := h.sessionUser(r)
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
		cName, err := query.New(h.DB).GetUserCategoryName(r.Context(), query.GetUserCategoryNameParams{ID: *in.CategoryID, UserID: sql.NullInt64{Int64: user.ID, Valid: true}})
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

	q := query.New(h.DB)
	row, err := q.CreateTransaction(r.Context(), query.CreateTransactionParams{UserID: user.ID, Name: in.Name, Amount: in.Amount, Type: txType, TransactionDate: txTime, CategoryID: categoryID, CategoryName: categoryName, BudgetID: budgetID, SubscriptionID: subID, Notes: notes})

	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	item := transactionDTO(row.ID, row.Name, row.Amount, row.Type, row.TransactionDate, row.CategoryID, row.CategoryName, row.BudgetID, sql.NullString{}, row.SubscriptionID, sql.NullString{}, row.Notes, row.CreatedAt)
	enrichTransactionNames(r.Context(), q, user.ID, &item)

	webutil.WriteJSON(w, http.StatusCreated, item)
}

func (h *TransactionsHandler) BulkCreateTransactions(w http.ResponseWriter, r *http.Request) {
	user, err := h.sessionUser(r)
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
	q := query.New(tx)

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
			cName, err := q.GetUserCategoryName(r.Context(), query.GetUserCategoryNameParams{ID: *in.CategoryID, UserID: sql.NullInt64{Int64: user.ID, Valid: true}})
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

		row, err := q.CreateTransaction(r.Context(), query.CreateTransactionParams{UserID: user.ID, Name: in.Name, Amount: in.Amount, Type: txType, TransactionDate: txTime, CategoryID: categoryID, CategoryName: categoryName, BudgetID: budgetID, SubscriptionID: subID, Notes: notes})
		if err != nil {
			webutil.ServerError(w, err)
			return
		}

		item := transactionDTO(row.ID, row.Name, row.Amount, row.Type, row.TransactionDate, row.CategoryID, row.CategoryName, row.BudgetID, sql.NullString{}, row.SubscriptionID, sql.NullString{}, row.Notes, row.CreatedAt)
		enrichTransactionNames(r.Context(), q, user.ID, &item)

		createdItems = append(createdItems, item)
	}

	if err := tx.Commit(); err != nil {
		webutil.ServerError(w, err)
		return
	}

	webutil.WriteJSON(w, http.StatusCreated, createdItems)
}

func (h *TransactionsHandler) UpdateTransaction(w http.ResponseWriter, r *http.Request) {
	user, err := h.sessionUser(r)
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
		cName, err := query.New(h.DB).GetUserCategoryName(r.Context(), query.GetUserCategoryNameParams{ID: *in.CategoryID, UserID: sql.NullInt64{Int64: user.ID, Valid: true}})
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

	q := query.New(h.DB)
	row, err := q.UpdateTransaction(r.Context(), query.UpdateTransactionParams{Name: in.Name, Amount: in.Amount, Type: txType, TransactionDate: txTime, CategoryID: categoryID, CategoryName: categoryName, BudgetID: budgetID, SubscriptionID: subID, Notes: notes, ID: txID, UserID: user.ID})

	if errors.Is(err, sql.ErrNoRows) {
		http.NotFound(w, r)
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	item := transactionDTO(row.ID, row.Name, row.Amount, row.Type, row.TransactionDate, row.CategoryID, row.CategoryName, row.BudgetID, sql.NullString{}, row.SubscriptionID, sql.NullString{}, row.Notes, row.CreatedAt)
	enrichTransactionNames(r.Context(), q, user.ID, &item)

	webutil.WriteJSON(w, http.StatusOK, item)
}

func (h *TransactionsHandler) DeleteTransaction(w http.ResponseWriter, r *http.Request) {
	user, err := h.sessionUser(r)
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

	deleted, err := query.New(h.DB).DeleteTransaction(r.Context(), query.DeleteTransactionParams{ID: txID, UserID: user.ID})
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
