package subscriptions

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
	token := "subscriptionstesttoken"
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

func TestCalculateMonthlyEquivalent(t *testing.T) {
	tests := []struct {
		amount float64
		cycle  string
		want   float64
	}{
		{amount: 100, cycle: "monthly", want: 100},
		{amount: 1200, cycle: "yearly", want: 100},
	}

	for _, tt := range tests {
		got := CalculateMonthlyEquivalent(tt.amount, tt.cycle)
		if got != tt.want {
			t.Errorf("CalculateMonthlyEquivalent(%f, %q) = %f, want %f", tt.amount, tt.cycle, got, tt.want)
		}
	}
}

func TestCalculateNextRenewalAllCadences(t *testing.T) {
	refTime := time.Date(2026, 8, 2, 0, 0, 0, 0, time.UTC) // Aug 2, 2026

	renDateDec15 := time.Date(2026, 12, 15, 0, 0, 0, 0, time.UTC)
	renDateJan15 := time.Date(2026, 1, 15, 0, 0, 0, 0, time.UTC)
	renDateAug2 := time.Date(2026, 8, 2, 0, 0, 0, 0, time.UTC)

	bDay1 := 1
	bDay2 := 2
	bDay15 := 15
	bDay31 := 31

	tests := []struct {
		name        string
		billingDay  *int
		renewalDate *time.Time
		cycle       string
		refTime     time.Time
		want        time.Time
	}{
		// --- MONTHLY ---
		{
			name:       "monthly: day 1 on Aug 2 -> Aug 1 passed -> Sept 1 (30 days away)",
			billingDay: &bDay1,
			cycle:      "monthly",
			refTime:    refTime,
			want:       time.Date(2026, 9, 1, 0, 0, 0, 0, time.UTC),
		},
		{
			name:       "monthly: day 2 on Aug 2 -> due today (Aug 2)",
			billingDay: &bDay2,
			cycle:      "monthly",
			refTime:    refTime,
			want:       time.Date(2026, 8, 2, 0, 0, 0, 0, time.UTC),
		},
		{
			name:       "monthly: day 15 on Aug 2 -> in future (Aug 15)",
			billingDay: &bDay15,
			cycle:      "monthly",
			refTime:    refTime,
			want:       time.Date(2026, 8, 15, 0, 0, 0, 0, time.UTC),
		},
		{
			name:       "monthly: day 31 in April (30-day month) -> clamps to April 30",
			billingDay: &bDay31,
			cycle:      "monthly",
			refTime:    time.Date(2026, 4, 10, 0, 0, 0, 0, time.UTC),
			want:       time.Date(2026, 4, 30, 0, 0, 0, 0, time.UTC),
		},

		// --- YEARLY ---
		{
			name:        "yearly: Dec 15 renewal date evaluated on Aug 2 -> Dec 15, 2026",
			renewalDate: &renDateDec15,
			cycle:       "yearly",
			refTime:     refTime,
			want:        time.Date(2026, 12, 15, 0, 0, 0, 0, time.UTC),
		},
		{
			name:        "yearly: Jan 15 renewal date evaluated on Aug 2 -> Jan 15, 2027 (next year)",
			renewalDate: &renDateJan15,
			cycle:       "yearly",
			refTime:     refTime,
			want:        time.Date(2027, 1, 15, 0, 0, 0, 0, time.UTC),
		},
		{
			name:        "yearly: Aug 2 renewal date evaluated on Aug 2 -> Aug 2, 2026 (due today)",
			renewalDate: &renDateAug2,
			cycle:       "yearly",
			refTime:     refTime,
			want:        time.Date(2026, 8, 2, 0, 0, 0, 0, time.UTC),
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := CalculateNextRenewal(tt.billingDay, tt.renewalDate, tt.cycle, tt.refTime)
			if !got.Equal(tt.want) {
				t.Errorf("CalculateNextRenewal(%v, %v, %q, %v) = %v, want %v", tt.billingDay, tt.renewalDate, tt.cycle, tt.refTime, got, tt.want)
			}
		})
	}
}

func TestSubscriptionsCRUD(t *testing.T) {
	db, shutdown := testhelper.StartPostgres(t)
	defer shutdown()

	h := NewHandler(db)
	cookie := loginUser(t, db)

	var subID int64
	{
		billingDay := 15
		notes := "Family plan"
		body, _ := json.Marshal(SubscriptionInput{
			Name:         "Netflix Premium",
			Amount:       649.0,
			BillingCycle: "monthly",
			BillingDay:   &billingDay,
			Notes:        &notes,
		})
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodPost, "/api/horizon/subscriptions", bytes.NewReader(body))
		req.AddCookie(cookie)
		h.CreateSubscription(rec, req)

		if rec.Code != http.StatusCreated {
			t.Fatalf("expected 201 Created, got %d", rec.Code)
		}

		var sub SubscriptionDTO
		if err := json.NewDecoder(rec.Body).Decode(&sub); err != nil {
			t.Fatalf("failed to decode response: %v", err)
		}
		if sub.Name != "Netflix Premium" || sub.Amount != 649.0 || sub.Status != "active" {
			t.Errorf("unexpected subscription DTO: %+v", sub)
		}
		subID = sub.ID
	}

	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodGet, "/api/horizon/subscriptions", nil)
		req.AddCookie(cookie)
		h.ListSubscriptions(rec, req)

		if rec.Code != http.StatusOK {
			t.Fatalf("expected 200 OK, got %d", rec.Code)
		}

		var summary SubscriptionSummaryDTO
		_ = json.NewDecoder(rec.Body).Decode(&summary)
		if summary.ActiveSubscriptionsCount != 1 || summary.TotalMonthlyBurn != 649.0 {
			t.Errorf("unexpected summary DTO: %+v", summary)
		}
	}

	{
		paused := "paused"
		billingDay := 15
		body, _ := json.Marshal(SubscriptionInput{
			Name:         "Netflix Premium",
			Amount:       649.0,
			BillingCycle: "monthly",
			BillingDay:   &billingDay,
			Status:       &paused,
		})
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodPut, "/api/horizon/subscriptions/{id}", bytes.NewReader(body))
		req.AddCookie(cookie)

		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(subID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.UpdateSubscription(rec, req)
		if rec.Code != http.StatusOK {
			t.Fatalf("expected 200 OK, got %d", rec.Code)
		}
	}

	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodDelete, "/api/horizon/subscriptions/{id}", nil)
		req.AddCookie(cookie)

		rctx := chi.NewRouteContext()
		rctx.URLParams.Add("id", strconv.FormatInt(subID, 10))
		req = req.WithContext(context.WithValue(req.Context(), chi.RouteCtxKey, rctx))

		h.DeleteSubscription(rec, req)
		if rec.Code != http.StatusNoContent {
			t.Fatalf("expected 204 No Content, got %d", rec.Code)
		}
	}
}
