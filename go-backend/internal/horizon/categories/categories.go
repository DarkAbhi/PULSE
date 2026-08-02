package categories

import (
	"database/sql"
	"encoding/json"
	"errors"
	"net/http"
	"strings"

	"github.com/DarkAbhi/life-backend/internal/auth"
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

type Handler struct {
	DB *sql.DB
}

func NewHandler(db *sql.DB) *Handler {
	return &Handler{DB: db}
}

func (h *Handler) FetchCategories(userID int64) ([]CategoryDTO, error) {
	rows, err := h.DB.Query(`
		SELECT id, name, icon, color, is_default, user_id
		FROM financial_horizon_categories
		WHERE user_id IS NULL OR user_id = $1
		ORDER BY is_default DESC, name ASC
	`, userID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	categories := make([]CategoryDTO, 0)
	for rows.Next() {
		var c CategoryDTO
		var uid sql.NullInt64
		if err := rows.Scan(&c.ID, &c.Name, &c.Icon, &c.Color, &c.IsDefault, &uid); err != nil {
			return nil, err
		}
		if uid.Valid {
			u := uid.Int64
			c.UserID = &u
		}
		categories = append(categories, c)
	}
	return categories, nil
}

func (h *Handler) ListCategories(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
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

func (h *Handler) CreateCategory(w http.ResponseWriter, r *http.Request) {
	user, err := auth.GetSessionUser(h.DB, r)
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

	var c CategoryDTO
	var userID sql.NullInt64
	err = h.DB.QueryRow(`
		INSERT INTO financial_horizon_categories (user_id, name, icon, color, is_default)
		VALUES ($1, $2, $3, $4, false)
		RETURNING id, name, icon, color, is_default, user_id
	`, user.ID, in.Name, icon, color).Scan(&c.ID, &c.Name, &c.Icon, &c.Color, &c.IsDefault, &userID)

	if err != nil {
		webutil.ServerError(w, err)
		return
	}

	if userID.Valid {
		u := userID.Int64
		c.UserID = &u
	}

	webutil.WriteJSON(w, http.StatusCreated, c)
}

func SeedDefaultCategories(db *sql.DB) error {
	var count int
	err := db.QueryRow(`SELECT COUNT(*) FROM financial_horizon_categories WHERE is_default = true`).Scan(&count)
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
		_, _ = db.Exec(`
			INSERT INTO financial_horizon_categories (name, icon, color, is_default)
			VALUES ($1, $2, $3, true)
			ON CONFLICT DO NOTHING
		`, c.name, c.icon, c.color)
	}
	return nil
}
