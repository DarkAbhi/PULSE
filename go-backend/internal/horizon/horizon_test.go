package horizon

import (
	"bytes"
	"context"
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strconv"
	"testing"
	"time"

	"github.com/go-chi/chi/v5"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
)

func loginUser(t *testing.T, db *sql.DB) *http.Cookie {
	t.Helper()
	token := "horizontesttoken"
	hash := sha256.Sum256([]byte(token))
	hashStr := hex.EncodeToString(hash[:])
	expiresAt := time.Now().Add(24 * time.Hour)
	_, err := db.Exec(`
		INSERT INTO user_sessions (user_id, token_hash, expires_at)
		VALUES (1, $1, $2)
	`, hashStr, expiresAt)
	if err != nil {
		t.Fatalf("failed to insert session: %v", err)
	}
	return &http.Cookie{
		Name:  "life_session",
		Value: token,
	}
}

func TestGetHorizonDefault(t *testing.T) {
	db, shutdown := testhelper.StartPostgres(t)
	defer shutdown()

	h := NewHandler(db)
	cookie := loginUser(t, db)

	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/api/horizon", nil)
	req.AddCookie(cookie)
	h.GetHorizon(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("expected 200 OK, got %d", rec.Code)
	}

	var out HorizonSummaryDTO
	if err := json.NewDecoder(rec.Body).Decode(&out); err != nil {
		t.Fatalf("failed to decode response: %v", err)
	}

	if out.BaseAmount != 0 || out.RemainingAmount != 0 || out.TotalDeductions != 0 {
		t.Errorf("expected empty horizon summary, got %+v", out)
	}
	if len(out.Projections) != 3 {
		t.Errorf("expected 3 projections, got %d", len(out.Projections))
	}
}

func TestUpdateConfigAndCalculations(t *testing.T) {
	db, shutdown := testhelper.StartPostgres(t)
	defer shutdown()

	h := NewHandler(db)
	cookie := loginUser(t, db)

	// 1. Update Config with base_amount = 100000
	{
		rec := httptest.NewRecorder()
		curr := "₹"
		body, _ := json.Marshal(ConfigInput{BaseAmount: 100000.0, Currency: &curr})
		req := httptest.NewRequest(http.MethodPut, "/api/horizon/config", bytes.NewReader(body))
		req.AddCookie(cookie)
		h.UpdateConfig(rec, req)

		if rec.Code != http.StatusOK {
			t.Fatalf("expected 200 OK, got %d", rec.Code)
		}

		var out HorizonSummaryDTO
		_ = json.NewDecoder(rec.Body).Decode(&out)
		if out.BaseAmount != 100000.0 || out.RemainingAmount != 100000.0 {
			t.Errorf("unexpected summary after config update: %+v", out)
		}
	}

	// 2. Add Deductions (Rent = 30000, SIP = 20000)
	{
		day := 5
		body, _ := json.Marshal(DeductionInput{Name: "Rent", Category: "housing", Amount: 30000.0, DueDay: &day})
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodPost, "/api/horizon/deductions", bytes.NewReader(body))
		req.AddCookie(cookie)
		h.CreateDeduction(rec, req)
		if rec.Code != http.StatusCreated {
			t.Fatalf("expected 201 Created, got %d", rec.Code)
		}
	}

	{
		day := 10
		body, _ := json.Marshal(DeductionInput{Name: "SIP Investment", Category: "investment", Amount: 20000.0, DueDay: &day})
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodPost, "/api/horizon/deductions", bytes.NewReader(body))
		req.AddCookie(cookie)
		h.CreateDeduction(rec, req)
		if rec.Code != http.StatusCreated {
			t.Fatalf("expected 201 Created, got %d", rec.Code)
		}
	}

	// 3. Verify Calculations: Total = 50000, Remaining = 50000, Ratio = 50%
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodGet, "/api/horizon", nil)
		req.AddCookie(cookie)
		h.GetHorizon(rec, req)

		var out HorizonSummaryDTO
		_ = json.NewDecoder(rec.Body).Decode(&out)
		if out.TotalDeductions != 50000.0 || out.RemainingAmount != 50000.0 || out.CommittedRatio != 50.0 {
			t.Errorf("unexpected calculations: %+v", out)
		}
		if len(out.Deductions) != 2 {
			t.Errorf("expected 2 deductions, got %d", len(out.Deductions))
		}
		if out.Projections[0].CumulativeUncommitted != 150000.0 { // 3 * 50000
			t.Errorf("expected 3-month projection of 150000, got %f", out.Projections[0].CumulativeUncommitted)
		}
	}
}

func TestUpdateAndDeleteDeduction(t *testing.T) {
	db, shutdown := testhelper.StartPostgres(t)
	defer shutdown()

	h := NewHandler(db)
	cookie := loginUser(t, db)

	// Create deduction
	var deductionID int64
	{
		body, _ := json.Marshal(DeductionInput{Name: "Internet", Category: "bill", Amount: 2000.0})
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodPost, "/api/horizon/deductions", bytes.NewReader(body))
		req.AddCookie(cookie)
		h.CreateDeduction(rec, req)

		var item DeductionDTO
		_ = json.NewDecoder(rec.Body).Decode(&item)
		deductionID = item.ID
	}

	// Update deduction (Change amount to 2500, toggle active off)
	{
		active := false
		body, _ := json.Marshal(DeductionInput{Name: "Fiber Internet", Category: "bill", Amount: 2500.0, IsActive: &active})
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodPut, "/api/horizon/deductions/{id}", bytes.NewReader(body))
		req.AddCookie(cookie)

		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(deductionID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.UpdateDeduction(rec, req)
		if rec.Code != http.StatusOK {
			t.Fatalf("expected 200 OK, got %d", rec.Code)
		}
	}

	// Delete deduction
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodDelete, "/api/horizon/deductions/{id}", nil)
		req.AddCookie(cookie)

		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(deductionID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.DeleteDeduction(rec, req)
		if rec.Code != http.StatusNoContent {
			t.Fatalf("expected 204 No Content, got %d", rec.Code)
		}
	}
}

func TestBudgetsAndDeductionLinking(t *testing.T) {
	db, shutdown := testhelper.StartPostgres(t)
	defer shutdown()

	h := NewHandler(db)
	cookie := loginUser(t, db)

	// 1. Create a Budget
	var budgetID int64
	{
		body, _ := json.Marshal(BudgetInput{Name: "Housing & Bills", AllocatedAmount: 50000.0})
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodPost, "/api/horizon/budgets", bytes.NewReader(body))
		req.AddCookie(cookie)
		h.CreateBudget(rec, req)

		if rec.Code != http.StatusCreated {
			t.Fatalf("expected 201 Created, got %d", rec.Code)
		}

		var b BudgetDTO
		_ = json.NewDecoder(rec.Body).Decode(&b)
		budgetID = b.ID
		if b.AllocatedAmount != 50000.0 || b.AvailableAmount != 50000.0 || b.UsedAmount != 0 {
			t.Errorf("unexpected initial budget DTO: %+v", b)
		}
	}

	// 2. Create a deduction linked to the budget
	{
		body, _ := json.Marshal(DeductionInput{
			Name:     "Rent Payment",
			Category: "housing",
			Amount:   30000.0,
			BudgetID: &budgetID,
		})
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodPost, "/api/horizon/deductions", bytes.NewReader(body))
		req.AddCookie(cookie)
		h.CreateDeduction(rec, req)

		if rec.Code != http.StatusCreated {
			t.Fatalf("expected 201 Created, got %d", rec.Code)
		}
	}

	// 3. Verify Horizon summary calculates budget used and available amounts correctly
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodGet, "/api/horizon", nil)
		req.AddCookie(cookie)
		h.GetHorizon(rec, req)

		var out HorizonSummaryDTO
		_ = json.NewDecoder(rec.Body).Decode(&out)
		if len(out.Budgets) != 1 {
			t.Fatalf("expected 1 budget, got %d", len(out.Budgets))
		}
		b := out.Budgets[0]
		if b.AllocatedAmount != 50000.0 || b.UsedAmount != 30000.0 || b.AvailableAmount != 20000.0 || b.UsagePercentage != 60.0 {
			t.Errorf("unexpected budget calculations: %+v", b)
		}
	}

	// 4. Delete budget and ensure summary does not fail and unlinks budget
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodDelete, "/api/horizon/budgets/{id}", nil)
		req.AddCookie(cookie)

		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(budgetID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.DeleteBudget(rec, req)
		if rec.Code != http.StatusNoContent {
			t.Fatalf("expected 204 No Content, got %d", rec.Code)
		}
	}
}

func TestCategoriesAndTransactions(t *testing.T) {
	db, shutdown := testhelper.StartPostgres(t)
	defer shutdown()

	h := NewHandler(db)
	cookie := loginUser(t, db)

	// 1. List Categories (should auto-seed default categories)
	var defaultCategoryID int64
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodGet, "/api/horizon/categories", nil)
		req.AddCookie(cookie)
		h.ListCategories(rec, req)

		if rec.Code != http.StatusOK {
			t.Fatalf("expected 200 OK, got %d", rec.Code)
		}

		var categories []CategoryDTO
		if err := json.NewDecoder(rec.Body).Decode(&categories); err != nil {
			t.Fatalf("failed to decode categories: %v", err)
		}
		if len(categories) < 10 {
			t.Errorf("expected at least 10 default categories, got %d", len(categories))
		}
		defaultCategoryID = categories[0].ID
	}

	// 2. Create Custom Category
	var customCategoryID int64
	{
		icon := "laptop"
		color := "#10b981"
		body, _ := json.Marshal(CategoryInput{Name: "Gadgets & Electronics", Icon: &icon, Color: &color})
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodPost, "/api/horizon/categories", bytes.NewReader(body))
		req.AddCookie(cookie)
		h.CreateCategory(rec, req)

		if rec.Code != http.StatusCreated {
			t.Fatalf("expected 201 Created, got %d", rec.Code)
		}

		var c CategoryDTO
		_ = json.NewDecoder(rec.Body).Decode(&c)
		if c.Name != "Gadgets & Electronics" || c.IsDefault != false {
			t.Errorf("unexpected category DTO: %+v", c)
		}
		customCategoryID = c.ID
	}

	// 3. Create Transaction
	var transactionID int64
	{
		notes := "Bought new Wireless Headphones"
		body, _ := json.Marshal(TransactionInput{
			Name:       "Sony Headphones",
			Amount:     15000.0,
			CategoryID: &customCategoryID,
			Notes:      &notes,
		})
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodPost, "/api/horizon/transactions", bytes.NewReader(body))
		req.AddCookie(cookie)
		h.CreateTransaction(rec, req)

		if rec.Code != http.StatusCreated {
			t.Fatalf("expected 201 Created, got %d", rec.Code)
		}

		var tx TransactionDTO
		_ = json.NewDecoder(rec.Body).Decode(&tx)
		if tx.Name != "Sony Headphones" || tx.Amount != 15000.0 || tx.CategoryName != "Gadgets & Electronics" {
			t.Errorf("unexpected transaction DTO: %+v", tx)
		}
		transactionID = tx.ID
	}

	// 4. List Transactions
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodGet, "/api/horizon/transactions", nil)
		req.AddCookie(cookie)
		h.ListTransactions(rec, req)

		if rec.Code != http.StatusOK {
			t.Fatalf("expected 200 OK, got %d", rec.Code)
		}

		var transactions []TransactionDTO
		_ = json.NewDecoder(rec.Body).Decode(&transactions)
		if len(transactions) != 1 {
			t.Errorf("expected 1 transaction, got %d", len(transactions))
		}
	}

	// 4b. Bulk Create Transactions
	{
		bulkBody, _ := json.Marshal([]TransactionInput{
			{Name: "Bulk Item 1", Amount: 500.0},
			{Name: "Bulk Item 2", Amount: 1200.0},
		})
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodPost, "/api/horizon/transactions/bulk", bytes.NewReader(bulkBody))
		req.AddCookie(cookie)
		h.BulkCreateTransactions(rec, req)

		if rec.Code != http.StatusCreated {
			t.Fatalf("expected 201 Created for bulk insert, got %d", rec.Code)
		}

		var bulkTx []TransactionDTO
		_ = json.NewDecoder(rec.Body).Decode(&bulkTx)
		if len(bulkTx) != 2 {
			t.Errorf("expected 2 bulk transactions, got %d", len(bulkTx))
		}
	}

	// 5. Update Transaction
	{
		notes := "Updated price after discount"
		body, _ := json.Marshal(TransactionInput{
			Name:       "Sony Headphones Pro",
			Amount:     13500.0,
			CategoryID: &defaultCategoryID,
			Notes:      &notes,
		})
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodPut, "/api/horizon/transactions/{id}", bytes.NewReader(body))
		req.AddCookie(cookie)

		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(transactionID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.UpdateTransaction(rec, req)
		if rec.Code != http.StatusOK {
			t.Fatalf("expected 200 OK, got %d", rec.Code)
		}

		var tx TransactionDTO
		_ = json.NewDecoder(rec.Body).Decode(&tx)
		if tx.Amount != 13500.0 || tx.Name != "Sony Headphones Pro" {
			t.Errorf("unexpected updated transaction: %+v", tx)
		}
	}

	// 6. Delete Transaction
	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodDelete, "/api/horizon/transactions/{id}", nil)
		req.AddCookie(cookie)

		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(transactionID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.DeleteTransaction(rec, req)
		if rec.Code != http.StatusNoContent {
			t.Fatalf("expected 204 No Content, got %d", rec.Code)
		}
	}
}
