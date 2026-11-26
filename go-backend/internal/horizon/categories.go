package horizon

import (
	"context"
	"database/sql"
	"encoding/json"
	"errors"
	"net/http"
	"strings"

	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/DarkAbhi/life-backend/internal/auth"
	"github.com/DarkAbhi/life-backend/internal/horizon/query"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type CategoryInput struct {
	Name     string  `json:"name"`
	Icon     *string `json:"icon"`
	Color    *string `json:"color"`
	ParentID *int64  `json:"parent_id"`
}

type CategoryDTO struct {
	ID                    int64  `json:"id"`
	Name                  string `json:"name"`
	Icon                  string `json:"icon"`
	Color                 string `json:"color"`
	IsDefault             bool   `json:"is_default"`
	UserID                *int64 `json:"user_id,omitempty"`
	ParentID              *int64 `json:"parent_id"`
	AllowsSubcategories   bool   `json:"allows_subcategories"`
	IsSubscriptionDefault bool   `json:"is_subscription_default,omitempty"`
}

func categoryDTO(id int64, name, icon, color string, isDefault bool, userID, parentID sql.NullInt64, allowsSubcategories bool) CategoryDTO {
	category := CategoryDTO{
		ID: id, Name: name, Icon: icon, Color: color, IsDefault: isDefault,
		AllowsSubcategories:   allowsSubcategories,
		IsSubscriptionDefault: isDefault && !parentID.Valid && name == "Subscription",
	}
	if userID.Valid {
		category.UserID = &userID.Int64
	}
	if parentID.Valid {
		category.ParentID = &parentID.Int64
	}
	return category
}

type CategoriesHandler struct {
	sessions SessionLookup
	DB       *pgxpool.Pool
}

func NewCategoriesHandler(db *pgxpool.Pool, sessions SessionLookup) *CategoriesHandler {
	return &CategoriesHandler{DB: db, sessions: sessions}
}
func (h *CategoriesHandler) sessionUser(r *http.Request) (auth.SessionUser, error) {
	return h.sessions(r)
}

func (h *CategoriesHandler) FetchCategories(ctx context.Context, userID int64) ([]CategoryDTO, error) {
	rows, err := query.New(h.DB).ListCategories(ctx, sql.NullInt64{Int64: userID, Valid: true})
	if err != nil {
		return nil, err
	}

	categories := make([]CategoryDTO, 0, len(rows))
	for _, row := range rows {
		categories = append(categories, categoryDTO(row.ID, row.Name, row.Icon, row.Color, row.IsDefault, row.UserID, row.ParentID, row.AllowsSubcategories))
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

	categories, err := h.FetchCategories(r.Context(), user.ID)
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
		webutil.BadRequest(w, "invalid json")
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

	var parentID sql.NullInt64
	if in.ParentID != nil {
		parentID = sql.NullInt64{Int64: *in.ParentID, Valid: true}
	}
	row, err := query.New(h.DB).CreateCategory(r.Context(), query.CreateCategoryParams{
		UserID: sql.NullInt64{Int64: user.ID, Valid: true},
		Name:   in.Name, Icon: icon, Color: color, ParentID: parentID,
	})
	if err != nil {
		var pgErr *pgconn.PgError
		switch {
		case errors.Is(err, sql.ErrNoRows):
			webutil.BadRequest(w, "select a top-level category that allows subcategories")
		case errors.As(err, &pgErr) && pgErr.Code == "23505":
			webutil.BadRequest(w, "a category with that name already exists at this level")
		default:
			webutil.ServerError(w, err)
		}
		return
	}

	webutil.WriteJSON(w, http.StatusCreated, categoryDTO(row.ID, row.Name, row.Icon, row.Color, row.IsDefault, row.UserID, row.ParentID, row.AllowsSubcategories))
}
