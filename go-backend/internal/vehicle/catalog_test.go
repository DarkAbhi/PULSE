//go:build integration

package vehicle

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"testing"

	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/DarkAbhi/life-backend/internal/testhelper"
	"github.com/go-chi/chi/v5"
)

func TestCatalogVehicleLifecycle(t *testing.T) {
	_, dsn, stop := testhelper.StartPostgresWithDSN(t)
	defer stop()
	pool := newTestPool(t, dsn)
	defer pool.Close()
	userID := int64(1)
	handler := NewHandler(pool, NewService(NewRepository(pool)), func(*http.Request) (auth.SessionUser, error) {
		return auth.SessionUser{ID: userID}, nil
	})
	router := chi.NewRouter()
	handler.RegisterRoutes(router)
	request := func(method, path, body string, status int) *httptest.ResponseRecorder {
		t.Helper()
		response := httptest.NewRecorder()
		router.ServeHTTP(response, httptest.NewRequest(method, path, strings.NewReader(body)))
		if response.Code != status {
			t.Fatalf("%s %s: got %d, want %d: %s", method, path, response.Code, status, response.Body.String())
		}
		return response
	}
	for _, tc := range []struct {
		name, path string
		status     int
	}{
		{"kinds", "/vehicle-catalog/kinds", 200},
		{"make search", "/vehicle-catalog/makes?kind=car&q=honda", 200},
		{"model search", "/vehicle-catalog/models?kind=car&make_slug=honda&q=civic", 200},
		{"invalid kind", "/vehicle-catalog/makes?kind=spaceship", 400},
		{"missing make", "/vehicle-catalog/models?kind=car", 400},
		{"search bound", "/vehicle-catalog/makes?kind=car&q=" + strings.Repeat("a", 101), 400},
	} {
		t.Run(tc.name, func(t *testing.T) { request("GET", tc.path, "", tc.status) })
	}
	var options []CatalogOption
	response := request("GET", "/vehicle-catalog/models?kind=car&make_slug=honda&q=civic", "", 200)
	if err := json.Unmarshal(response.Body.Bytes(), &options); err != nil {
		t.Fatal(err)
	}
	found := false
	for _, option := range options {
		if option.ID == "car/honda/civic" {
			found = true
		}
	}
	if !found {
		t.Fatal("pinned Honda Civic missing")
	}
	if body := request("GET", "/vehicle-catalog/makes?kind=car&q=%25", "", 200).Body.String(); body != "[]\n" {
		t.Fatalf("wildcard search was not literal: %s", body)
	}
	request("POST", "/vehicles", `{"name":"bad","kind":"motorcycle","catalog_id":"car/honda/civic","fuel_types":["petrol"]}`, 400)
	for _, tc := range []struct {
		name, kind, makeName, modelName string
		status                          int
		wantMatch                       bool
	}{
		{"exact", "car", "Honda", "Civic", 200, true},
		{"case and space", "car", "  HONDA ", " civic ", 200, true},
		{"no fuzzy guessing", "car", "Honda", "Civi", 200, false},
		{"literal wildcards", "car", "Honda", "%", 200, false},
		{"wrong kind", "bus", "Honda", "Civic", 200, false},
		{"invalid kind", "spaceship", "Honda", "Civic", 400, false},
		{"blank", "car", "  ", "Civic", 400, false},
		{"bounded", "car", "Honda", strings.Repeat("a", 201), 400, false},
	} {
		t.Run("match/"+tc.name, func(t *testing.T) {
			path := "/vehicle-catalog/match?" + url.Values{"kind": {tc.kind}, "make_name": {tc.makeName}, "model_name": {tc.modelName}}.Encode()
			result := request("GET", path, "", tc.status)
			if tc.status != 200 {
				return
			}
			var body CatalogMatchResponse
			if err := json.Unmarshal(result.Body.Bytes(), &body); err != nil {
				t.Fatal(err)
			}
			if (body.Match != nil) != tc.wantMatch {
				t.Fatalf("unexpected match: %s", result.Body.String())
			}
			if body.Match != nil && (body.Match.ID != "car/honda/civic" || body.Match.MakeName != "Honda" || body.Match.ModelName != "Civic" || body.Match.MakeSlug != "honda") {
				t.Fatalf("noncanonical match: %+v", body.Match)
			}
		})
	}
	if _, err := pool.Exec(t.Context(), `INSERT INTO vehicle_catalog (id,kind,make_slug,make_name,model_slug,model_name,aliases)
		VALUES ('car/honda/civic-duplicate','car','honda','Honda','civic-duplicate','Civic','')`); err != nil {
		t.Fatal(err)
	}
	if result := request("GET", "/vehicle-catalog/match?kind=car&make_name=Honda&model_name=Civic", "", 200); strings.TrimSpace(result.Body.String()) != `{"match":null}` {
		t.Fatalf("ambiguous match suggested: %s", result.Body.String())
	}
	request("POST", "/vehicles", `{"name":"bad","kind":"car","catalog_id":"car/missing/model","fuel_types":["petrol"]}`, 400)
	request("POST", "/vehicles", `{"name":"old Android contract","vehicle_type":"four_wheeler","fuel_types":["petrol"]}`, 400)
	response = request("POST", "/vehicles", `{"name":"My car","kind":"car","catalog_id":"car/honda/civic","make_name":"Forged","model_name":"Forged","fuel_types":["petrol","cng"]}`, 201)
	var vehicle DTO
	if err := json.Unmarshal(response.Body.Bytes(), &vehicle); err != nil {
		t.Fatal(err)
	}
	if vehicle.MakeName != "Honda" || vehicle.ModelName != "Civic" {
		t.Fatalf("untrusted display labels saved: %+v", vehicle)
	}
	path := fmt.Sprintf("/vehicles/%d", vehicle.ID)
	response = request("PATCH", path, `{"name":"New nickname"}`, 200)
	if err := json.Unmarshal(response.Body.Bytes(), &vehicle); err != nil {
		t.Fatal(err)
	}
	if vehicle.CatalogID == nil || *vehicle.CatalogID != "car/honda/civic" {
		t.Fatal("unrelated update lost catalog identity")
	}
	request("PATCH", path, `{"kind":"bus"}`, 400)
	request("PATCH", path, `{"catalog_id":"","kind":"bus","make_name":"Custom make","model_name":"Custom model"}`, 200)
	response = request("GET", path+"/history", "", 200)
	var history struct {
		Kind      string  `json:"kind"`
		CatalogID *string `json:"catalog_id"`
		MakeName  string  `json:"make_name"`
	}
	if err := json.Unmarshal(response.Body.Bytes(), &history); err != nil {
		t.Fatal(err)
	}
	if history.Kind != "bus" || history.CatalogID != nil || history.MakeName != "Custom make" {
		t.Fatalf("manual identity not preserved in history: %+v", history)
	}
	userID = 987654
	request("GET", path, "", 404)
}

func TestCatalogRequiresSession(t *testing.T) {
	handler := NewHandler(nil, nil, func(*http.Request) (auth.SessionUser, error) { return auth.SessionUser{}, sql.ErrNoRows })
	response := httptest.NewRecorder()
	handler.CatalogMatch(response, httptest.NewRequest("GET", "/vehicle-catalog/match?kind=car&make_name=Honda&model_name=Civic", nil))
	if response.Code != 401 {
		t.Fatalf("match without session: %d", response.Code)
	}
	for _, path := range []string{"/vehicle-catalog/kinds", "/vehicle-catalog/makes?kind=car", "/vehicle-catalog/models?kind=car&make_slug=honda"} {
		response := httptest.NewRecorder()
		handler.CatalogOptions(response, httptest.NewRequest("GET", path, nil))
		if response.Code != 401 {
			t.Fatalf("%s: got %d", path, response.Code)
		}
	}
}
