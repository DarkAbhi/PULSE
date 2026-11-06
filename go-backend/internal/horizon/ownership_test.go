package horizon

import (
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"github.com/go-chi/chi/v5"
)

func TestHorizonRejectsForeignReferences(t *testing.T) {
	db, dsn, stop := testhelper.StartPostgresWithDSN(t)
	defer stop()
	pool := testPool(t, dsn)
	defer pool.Close()
	var otherID int64
	if err := db.QueryRow(`INSERT INTO users (username,password_hash) VALUES ('foreign-owner','unused') RETURNING id`).Scan(&otherID); err != nil {
		t.Fatal(err)
	}
	token := "ownership-bearer-token"
	hash := sha256.Sum256([]byte(token))
	if _, err := db.Exec(`INSERT INTO user_sessions (user_id,token_hash,expires_at) VALUES (1,$1,NOW()+INTERVAL '1 hour')`, hex.EncodeToString(hash[:])); err != nil {
		t.Fatal(err)
	}
	ids := make(map[string]int64)
	for key, statement := range map[string]string{
		"category_id":     `INSERT INTO financial_horizon_categories (user_id,name) VALUES ($1,'private category') RETURNING id`,
		"budget_id":       `INSERT INTO financial_horizon_budgets (user_id,name,allocated_amount) VALUES ($1,'private budget',100) RETURNING id`,
		"deduction_id":    `INSERT INTO financial_horizon_deductions (user_id,name,amount) VALUES ($1,'private deduction',10) RETURNING id`,
		"subscription_id": `INSERT INTO financial_horizon_subscriptions (user_id,name,amount) VALUES ($1,'private subscription',10) RETURNING id`,
	} {
		var id int64
		if err := db.QueryRow(statement, otherID).Scan(&id); err != nil {
			t.Fatal(err)
		}
		ids[key] = id
	}
	router := chi.NewRouter()
	authService := auth.NewService(auth.NewRepository(pool))
	lookup := func(r *http.Request) (auth.SessionUser, error) {
		user, err := authService.SessionUser(r.Context(), auth.ExtractSessionToken(r))
		if errors.Is(err, auth.ErrSessionNotFound) {
			return auth.SessionUser{}, sql.ErrNoRows
		}
		return user, err
	}
	NewHandler(pool, NewService(pool), lookup).RegisterRoutes(router)
	requestAs := func(credential, method, path, body string) *httptest.ResponseRecorder {
		t.Helper()
		req := httptest.NewRequest(method, path, strings.NewReader(body))
		req.Header.Set("Authorization", "Bearer "+credential)
		rec := httptest.NewRecorder()
		router.ServeHTTP(rec, req)
		return rec
	}
	request := func(method, path, body string) *httptest.ResponseRecorder {
		return requestAs(token, method, path, body)
	}
	created := request(http.MethodPost, "/horizon/transactions", `{"name":"own transaction","amount":5,"user_id":999}`)
	if created.Code != http.StatusCreated {
		t.Fatalf("create own: %d %s", created.Code, created.Body.String())
	}
	var own TransactionDTO
	if err := json.Unmarshal(created.Body.Bytes(), &own); err != nil {
		t.Fatal(err)
	}
	var owner int64
	if err := db.QueryRow(`SELECT user_id FROM financial_horizon_transactions WHERE id=$1`, own.ID).Scan(&owner); err != nil || owner != 1 {
		t.Fatalf("forged owner saved: %d %v", owner, err)
	}
	otherToken := "second-owner-bearer-token"
	otherHash := sha256.Sum256([]byte(otherToken))
	if _, err := db.Exec(`INSERT INTO user_sessions (user_id,token_hash,expires_at) VALUES ($1,$2,NOW()+INTERVAL '1 hour')`, otherID, hex.EncodeToString(otherHash[:])); err != nil {
		t.Fatal(err)
	}
	for _, method := range []string{http.MethodPut, http.MethodDelete} {
		rec := requestAs(otherToken, method, fmt.Sprintf("/horizon/transactions/%d", own.ID), `{"name":"stolen","amount":5}`)
		if rec.Code != http.StatusNotFound {
			t.Fatalf("foreign %s: %d %s", method, rec.Code, rec.Body.String())
		}
	}
	listed := requestAs(otherToken, http.MethodGet, "/horizon/transactions", "")
	var page PaginatedTransactionsDTO
	if err := json.Unmarshal(listed.Body.Bytes(), &page); err != nil || listed.Code != http.StatusOK || page.Total != 0 {
		t.Fatalf("foreign list: %d %s %v", listed.Code, listed.Body.String(), err)
	}
	req := httptest.NewRequest(http.MethodGet, "/horizon/transactions", nil)
	req.Header.Set("Authorization", "Basic invalid")
	req.AddCookie(&http.Cookie{Name: auth.SessionCookieName, Value: token})
	invalid := httptest.NewRecorder()
	router.ServeHTTP(invalid, req)
	if invalid.Code != http.StatusUnauthorized {
		t.Fatalf("malformed header fell back to cookie: %d", invalid.Code)
	}
	var deductionID, subscriptionID int64
	if err := db.QueryRow(`INSERT INTO financial_horizon_deductions (user_id,name,amount) VALUES (1,'own',1) RETURNING id`).Scan(&deductionID); err != nil {
		t.Fatal(err)
	}
	if err := db.QueryRow(`INSERT INTO financial_horizon_subscriptions (user_id,name,amount) VALUES (1,'own',1) RETURNING id`).Scan(&subscriptionID); err != nil {
		t.Fatal(err)
	}
	for _, endpoint := range []struct {
		method, path string
		keys         []string
	}{
		{http.MethodPost, "/horizon/transactions", []string{"category_id", "budget_id", "subscription_id"}},
		{http.MethodPut, fmt.Sprintf("/horizon/transactions/%d", own.ID), []string{"category_id", "budget_id", "subscription_id"}},
		{http.MethodPost, "/horizon/subscriptions", []string{"category_id", "budget_id", "deduction_id"}},
		{http.MethodPut, fmt.Sprintf("/horizon/subscriptions/%d", subscriptionID), []string{"category_id", "budget_id", "deduction_id"}},
		{http.MethodPost, "/horizon/deductions", []string{"budget_id"}},
		{http.MethodPut, fmt.Sprintf("/horizon/deductions/%d", deductionID), []string{"budget_id"}},
	} {
		for _, key := range endpoint.keys {
			t.Run(endpoint.method+endpoint.path+key, func(t *testing.T) {
				rec := request(endpoint.method, endpoint.path, fmt.Sprintf(`{"name":"attempt","amount":5,"%s":%d}`, key, ids[key]))
				if rec.Code != http.StatusBadRequest {
					t.Fatalf("foreign reference: %d %s", rec.Code, rec.Body.String())
				}
			})
		}
	}
	bulk := request(http.MethodPost, "/horizon/transactions/bulk", fmt.Sprintf(`[{"name":"must rollback","amount":5},{"name":"foreign","amount":5,"budget_id":%d}]`, ids["budget_id"]))
	if bulk.Code != http.StatusBadRequest {
		t.Fatalf("bulk: %d %s", bulk.Code, bulk.Body.String())
	}
	var count int
	if err := db.QueryRow(`SELECT COUNT(*) FROM financial_horizon_transactions WHERE user_id=1`).Scan(&count); err != nil || count != 1 {
		t.Fatalf("bulk partially saved: count=%d err=%v", count, err)
	}
	var defaultID int64
	if err := db.QueryRow(`SELECT id FROM financial_horizon_categories WHERE user_id IS NULL AND is_default LIMIT 1`).Scan(&defaultID); err != nil {
		t.Fatal(err)
	}
	shared := request(http.MethodPost, "/horizon/transactions", fmt.Sprintf(`{"name":"shared default","amount":5,"category_id":%d}`, defaultID))
	if shared.Code != http.StatusCreated {
		t.Fatalf("shared category: %d %s", shared.Code, shared.Body.String())
	}
}
