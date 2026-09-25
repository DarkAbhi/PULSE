package horizon

import (
	"context"
	"database/sql"
	"encoding/json"
	"errors"
	"github.com/DarkAbhi/life-backend/internal/auth"
	"net/http"
	"strings"

	"github.com/DarkAbhi/life-backend/internal/horizon/query"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type CategoryInput struct {
	Name  string  `json:"name"`
	Icon  *string `json:"icon"`
	Color *string `json:"color"`
}

type CategoryDTO struct {
	ID        int64  `json:"id"`
	Name      string `json:"name"`
	Icon      string `json:"icon"`
	Color     string `json:"color"`
	IsDefault bool   `json:"is_default"`
	UserID    *int64 `json:"user_id,omitempty"`
}

type CategoriesHandler struct {
	sessions SessionLookup
	DB       *sql.DB
}

func NewCategoriesHandler(db *sql.DB, sessions SessionLookup) *CategoriesHandler {
	return &CategoriesHandler{DB: db, sessions: sessions}
}
func (h *CategoriesHandler) sessionUser(r *http.Request) (auth.SessionUser, error) {
	return h.sessions(r)
}

func (h *CategoriesHandler) FetchCategories(userID int64) ([]CategoryDTO, error) {
	rows, err := query.New(h.DB).ListCategories(context.Background(), sql.NullInt64{Int64: userID, Valid: true})
	if err != nil {
		return nil, err
	}

	categories := make([]CategoryDTO, 0)
	for _, row := range rows {
		c := CategoryDTO{ID: row.ID, Name: row.Name, Icon: row.Icon, Color: row.Color, IsDefault: row.IsDefault}
		if row.UserID.Valid {
			u := row.UserID.Int64
			c.UserID = &u
		}
		categories = append(categories, c)
	}
	return categories, nil
}

func (h *CategoriesHandler) ListCategories(w http.ResponseWriter, r *http.Request) {
	user, err := h.sessionUser(r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	_ = SeedDefaultCategories(h.DB)
	categories, err := h.FetchCategories(user.ID)
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	webutil.WriteJSON(w, http.StatusOK, categories)
}

func (h *CategoriesHandler) CreateCategory(w http.ResponseWriter, r *http.Request) {
	user, err := h.sessionUser(r)
	if errors.Is(err, sql.ErrNoRows) {
		webutil.Unauthorized(w, "session is invalid or expired")
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	var in CategoryInput
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<20)).Decode(&in); err != nil {
		webutil.BadRequest(w, "invalid JSON")
		return
	}

	in.Name = strings.TrimSpace(in.Name)
	if in.Name == "" || len([]rune(in.Name)) > 100 {
		webutil.BadRequest(w, "category name is required and must be under 100 characters")
		return
	}

	icon := "tag"
	if in.Icon != nil && strings.TrimSpace(*in.Icon) != "" {
		icon = strings.TrimSpace(*in.Icon)
	}

	color := "#64748b"
	if in.Color != nil && strings.TrimSpace(*in.Color) != "" {
		color = strings.TrimSpace(*in.Color)
	}

	row, err := query.New(h.DB).CreateCategory(r.Context(), query.CreateCategoryParams{UserID: sql.NullInt64{Int64: user.ID, Valid: true}, Name: in.Name, Icon: icon, Color: color})

	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	c := CategoryDTO{ID: row.ID, Name: row.Name, Icon: row.Icon, Color: row.Color, IsDefault: row.IsDefault}
	if row.UserID.Valid {
		u := row.UserID.Int64
		c.UserID = &u
	}

	webutil.WriteJSON(w, http.StatusCreated, c)
}

func SeedDefaultCategories(db *sql.DB) error {
	q := query.New(db)
	count, err := q.CountDefaultCategories(context.Background())
	if err != nil {
		return err
	}
	if count > 0 {
		return nil
	}

	defaultCategories := []struct {
		name  string
		icon  string
		color string
	}{
		{"Food & Dining", "utensils", "#f97316"},
		{"Bills & Utilities", "receipt", "#ef4444"},
		{"Housing", "home", "#8b5cf6"},
		{"Transportation", "car", "#3b82f6"},
		{"Shopping", "shopping-bag", "#ec4899"},
		{"Entertainment", "film", "#14b8a6"},
		{"Health & Fitness", "heart-pulse", "#06b6d4"},
		{"Investments & Savings", "trending-up", "#22c55e"},
		{"Subscriptions", "credit-card", "#6366f1"},
		{"Other", "tag", "#64748b"},
	}

	for _, c := range defaultCategories {
		_ = q.SeedDefaultCategory(context.Background(), query.SeedDefaultCategoryParams{Name: c.name, Icon: c.icon, Color: c.color})
	}
	return nil
}
