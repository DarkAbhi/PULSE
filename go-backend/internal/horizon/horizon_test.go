package horizon

import (
	"bytes"
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

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

func TestSummaryDefault(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()
	pool := testPool(t, dsn)
	defer pool.Close()

	h := NewHandler(pool, NewService(pool), testSessionLookup(db))
	cookie := loginUser(t, db)

	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/api/horizon", nil)
	req.AddCookie(cookie)
	h.Summary(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("expected 200 OK, got %d", rec.Code)
	}

	var out SummaryDTO
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
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()
	pool := testPool(t, dsn)
	defer pool.Close()

	h := NewHandler(pool, NewService(pool), testSessionLookup(db))
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

		var out SummaryDTO
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
		h.Summary(rec, req)

		var out SummaryDTO
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
