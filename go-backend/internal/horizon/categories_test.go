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

func categoriesLoginUser(t *testing.T, db *sql.DB) *http.Cookie {
	t.Helper()
	token := "categoriestesttoken"
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

func TestCategoriesListAndCreate(t *testing.T) {
	db, dsn, shutdown := testhelper.StartPostgresWithDSN(t)
	defer shutdown()
	pool := testPool(t, dsn)
	defer pool.Close()

	h := NewCategoriesHandler(pool, testSessionLookup(db))
	cookie := categoriesLoginUser(t, db)
	var food, transport, savings, takeaway int64

	{
		rec := httptest.NewRecorder()
		req := httptest.NewRequest(http.MethodGet, "/api/horizon/categories", nil)
		req.AddCookie(cookie)
		h.ListCategories(rec, req)

		if rec.Code != http.StatusOK {
			t.Fatalf("expected 200 OK, got %d", rec.Code)
		}

		var categoriesList []CategoryDTO
		if err := json.NewDecoder(rec.Body).Decode(&categoriesList); err != nil {
			t.Fatalf("failed to decode categories: %v", err)
		}
		var children, roots int
		for _, c := range categoriesList {
			if c.ParentID == nil {
				roots++
			} else {
				children++
				if c.AllowsSubcategories {
					t.Errorf("subcategory accepts children: %s", c.Name)
				}
			}
			switch c.Name {
			case "Food and Drinks":
				food = c.ID
			case "Transport":
				transport = c.ID
			case "Savings":
				savings = c.ID
				if c.AllowsSubcategories || c.ParentID != nil {
					t.Fatalf("Savings must be a standalone category: %+v", c)
				}
			case "takeaway":
				takeaway = c.ID
			}
		}
		if roots != 32 || children != 231 || food == 0 || transport == 0 || savings == 0 || takeaway == 0 {
			t.Fatalf("unexpected category catalog: %d roots, %d children", roots, children)
		}
		for _, c := range categoriesList {
			if c.ID == takeaway && (c.ParentID == nil || *c.ParentID != food) {
				t.Fatalf("takeaway missing its parent: %+v", c)
			}
		}
	}

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
		if c.Name != "Gadgets & Electronics" || c.IsDefault || c.ParentID != nil || !c.AllowsSubcategories {
			t.Errorf("unexpected category DTO: %+v", c)
		}
	}

	var foreign int64
	if err := db.QueryRow(`
		WITH owner AS (INSERT INTO users (username, password_hash) VALUES ('category-owner', 'test') RETURNING id)
		INSERT INTO financial_horizon_categories (name, user_id)
		SELECT 'Private root', id FROM owner RETURNING id
	`).Scan(&foreign); err != nil {
		t.Fatal(err)
	}
	for _, tc := range []struct {
		name   string
		parent int64
		want   int
	}{
		{"food custom child", food, http.StatusCreated},
		{"duplicate in same branch", food, http.StatusBadRequest},
		{"same name in a different branch", transport, http.StatusCreated},
		{"third level", takeaway, http.StatusBadRequest},
		{"standalone", savings, http.StatusBadRequest},
		{"other user's category", foreign, http.StatusBadRequest},
	} {
		t.Run(tc.name, func(t *testing.T) {
			body, _ := json.Marshal(CategoryInput{Name: "Custom detail", ParentID: &tc.parent})
			rec := httptest.NewRecorder()
			req := httptest.NewRequest(http.MethodPost, "/api/horizon/categories", bytes.NewReader(body))
			req.AddCookie(cookie)
			h.CreateCategory(rec, req)
			if rec.Code != tc.want {
				t.Fatalf("got status %d, want %d: %s", rec.Code, tc.want, rec.Body.String())
			}
			if tc.want == http.StatusCreated {
				var c CategoryDTO
				if err := json.NewDecoder(rec.Body).Decode(&c); err != nil || c.ParentID == nil || *c.ParentID != tc.parent || c.AllowsSubcategories {
					t.Fatalf("unexpected child: %+v, %v", c, err)
				}
			}
		})
	}
}
