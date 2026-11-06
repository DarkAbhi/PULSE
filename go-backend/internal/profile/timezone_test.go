package profile

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"github.com/jackc/pgx/v5/pgxpool"
)

func TestValidTimeZone(t *testing.T) {
	for _, zone := range []string{"UTC", "Asia/Kolkata", "America/New_York", "Pacific/Chatham", "Europe/London"} {
		if !validTimeZone(zone) {
			t.Errorf("rejected %q", zone)
		}
	}
	for _, zone := range []string{"", "Local", "UPC", "Invalid/Zone", "../UTC", "/usr/share/zoneinfo/UTC", " UTC ", "+05:30"} {
		if validTimeZone(zone) {
			t.Errorf("accepted %q", zone)
		}
	}
}

func TestTimeZonePreference(t *testing.T) {
	db, dsn, stop := testhelper.StartPostgresWithDSN(t)
	defer stop()
	pool, err := pgxpool.New(context.Background(), dsn)
	if err != nil {
		t.Fatal(err)
	}
	defer pool.Close()
	service := NewService(NewRepository(pool), nil)
	handler := NewHandler(service, func(*http.Request) (int64, error) { return 1, nil })
	show := func() map[string]any {
		t.Helper()
		rec := httptest.NewRecorder()
		handler.Show(rec, httptest.NewRequest(http.MethodGet, "/profile", nil))
		if rec.Code != http.StatusOK {
			t.Fatalf("show status %d: %s", rec.Code, rec.Body)
		}
		var out map[string]any
		if err := json.Unmarshal(rec.Body.Bytes(), &out); err != nil {
			t.Fatal(err)
		}
		return out
	}
	if out := show(); out["timezone"] != "UTC" || out["has_profile"] != false {
		t.Fatalf("default profile: %v", out)
	}
	save := func(zone string, status int) {
		t.Helper()
		body, _ := json.Marshal(map[string]string{"timezone": zone})
		rec := httptest.NewRecorder()
		handler.SaveTimeZone(rec, httptest.NewRequest(http.MethodPut, "/profile/timezone", strings.NewReader(string(body))))
		if rec.Code != status {
			t.Fatalf("save %q status %d: %s", zone, rec.Code, rec.Body)
		}
	}
	save("America/New_York", http.StatusOK)
	if out := show(); out["timezone"] != "America/New_York" || out["has_profile"] != false {
		t.Fatalf("timezone-only profile: %v", out)
	}
	if _, err := service.Save(context.Background(), 1, "Name"); err != nil {
		t.Fatal(err)
	}
	if out := show(); out["timezone"] != "America/New_York" || out["name"] != "Name" {
		t.Fatalf("name erased timezone: %v", out)
	}
	save("UPC", http.StatusBadRequest)
	if out := show(); out["timezone"] != "America/New_York" {
		t.Fatalf("invalid write changed preference: %v", out)
	}
	if _, err := db.Exec(`INSERT INTO users (id, username, password_hash) VALUES (2, 'timezone-other', 'unused')`); err != nil {
		t.Fatal(err)
	}
	zone, err := service.TimeZone(context.Background(), 2)
	if err != nil || zone != "UTC" {
		t.Fatalf("another user timezone %q, %v", zone, err)
	}
	save("UTC", http.StatusOK)
	if out := show(); out["name"] != "Name" {
		t.Fatalf("timezone erased name: %v", out)
	}
}

func TestSavedZoneUsesDaylightSaving(t *testing.T) {
	location, err := time.LoadLocation("America/New_York")
	if err != nil {
		t.Fatal(err)
	}
	winter := time.Date(2026, 1, 1, 12, 0, 0, 0, time.UTC).In(location)
	summer := time.Date(2026, 7, 1, 12, 0, 0, 0, time.UTC).In(location)
	if winter.Hour() != 7 || summer.Hour() != 8 {
		t.Fatalf("DST was not applied: %v, %v", winter, summer)
	}
}
