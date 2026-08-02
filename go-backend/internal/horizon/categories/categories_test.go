package categories

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
	db, shutdown := testhelper.StartPostgres(t)
	defer shutdown()

	h := NewHandler(db)
	cookie := loginUser(t, db)

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
		if len(categoriesList) < 10 {
			t.Errorf("expected at least 10 default categories, got %d", len(categoriesList))
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
		if c.Name != "Gadgets & Electronics" || c.IsDefault != false {
			t.Errorf("unexpected category DTO: %+v", c)
		}
	}
}
