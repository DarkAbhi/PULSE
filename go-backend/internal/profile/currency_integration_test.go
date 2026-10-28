//go:build integration

package profile

import (
	"context"
	"encoding/json"
	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/DarkAbhi/life-backend/internal/horizon"
	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"github.com/jackc/pgx/v5/pgxpool"
	"net/http"
	"net/http/httptest"
	"os"
	"strings"
	"testing"
)

func TestCurrencyPreference(t *testing.T) {
	db, dsn, stop := testhelper.StartPostgresWithDSN(t)
	defer stop()
	ctx := context.Background()
	pool, err := pgxpool.New(ctx, dsn)
	if err != nil {
		t.Fatal(err)
	}
	defer pool.Close()
	service := NewService(NewRepository(pool), nil)
	id := int64(1)
	handler := NewHandler(service, func(*http.Request) (int64, error) { return id, nil })
	show := func() map[string]any {
		t.Helper()
		rec := httptest.NewRecorder()
		handler.Show(rec, httptest.NewRequest("GET", "/profile", nil))
		if rec.Code != 200 {
			t.Fatalf("show: %d %s", rec.Code, rec.Body)
		}
		var out map[string]any
		if err := json.Unmarshal(rec.Body.Bytes(), &out); err != nil {
			t.Fatal(err)
		}
		return out
	}
	save := func(body string, status int) {
		t.Helper()
		rec := httptest.NewRecorder()
		handler.SaveCurrency(rec, httptest.NewRequest("PUT", "/profile/currency", strings.NewReader(body)))
		if rec.Code != status {
			t.Fatalf("save: %d %s", rec.Code, rec.Body)
		}
	}
	if out := show(); out["currency"] != "GBP" || out["has_profile"] != false {
		t.Fatalf("default: %v", out)
	}
	save(`{"currency":"INR"}`, 200)
	if _, err := service.Save(ctx, 1, "Asha"); err != nil {
		t.Fatal(err)
	}
	if err := service.SaveTimeZone(ctx, 1, "Asia/Kolkata"); err != nil {
		t.Fatal(err)
	}
	for _, body := range []string{`{`, `{}`, `{"currency":"ZZZ"}`, `{"currency":"gbp"}`, `{"currency":"£"}`} {
		save(body, 400)
	}
	if out := show(); out["currency"] != "INR" || out["name"] != "Asha" || out["timezone"] != "Asia/Kolkata" {
		t.Fatalf("preservation: %v", out)
	}
	if _, err := db.Exec(`INSERT INTO users (id,username,password_hash) VALUES (2,'currency-other','unused')`); err != nil {
		t.Fatal(err)
	}
	id = 2
	if out := show(); out["currency"] != "GBP" {
		t.Fatalf("account isolation: %v", out)
	}
	id = 1
	financial := horizon.NewService(pool)
	if err := financial.UpsertConfig(ctx, 1, 123.45, "USD"); err != nil {
		t.Fatal(err)
	}
	summary, err := financial.Summary(ctx, 1)
	if err != nil {
		t.Fatal(err)
	}
	if summary.Currency != "INR" || summary.BaseAmount != 123.45 {
		t.Fatalf("profile override: %+v", summary)
	}
	save(`{"currency":"JPY"}`, 200)
	summary, err = financial.Summary(ctx, 1)
	if err != nil {
		t.Fatal(err)
	}
	if summary.Currency != "JPY" || summary.BaseAmount != 123.45 {
		t.Fatalf("unexpected conversion: %+v", summary)
	}
	denied := NewHandler(service, func(*http.Request) (int64, error) { return 0, auth.ErrSessionNotFound })
	rec := httptest.NewRecorder()
	denied.SaveCurrency(rec, httptest.NewRequest("PUT", "/profile/currency", strings.NewReader(`{"currency":"USD"}`)))
	if rec.Code != 401 {
		t.Fatalf("unauthenticated write: %d", rec.Code)
	}
	// Reapply the migration after rolling it back to exercise legacy backfill.
	down, err := os.ReadFile("../../migrations/00026_profile_currency.down.sql")
	if err != nil {
		t.Fatal(err)
	}
	if _, err = db.Exec(string(down)); err != nil {
		t.Fatal(err)
	}
	if _, err = db.Exec(`UPDATE financial_horizon_configs SET currency='₹' WHERE user_id=1`); err != nil {
		t.Fatal(err)
	}
	up, err := os.ReadFile("../../migrations/00026_profile_currency.up.sql")
	if err != nil {
		t.Fatal(err)
	}
	if _, err = db.Exec(string(up)); err != nil {
		t.Fatal(err)
	}
	if code, err := service.Currency(ctx, 1); err != nil || code != "INR" {
		t.Fatalf("legacy backfill: %s %v", code, err)
	}
}
