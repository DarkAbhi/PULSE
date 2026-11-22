package mealplan

import (
	"context"
	"database/sql"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"strconv"
	"strings"
	"unicode/utf8"

	"github.com/go-chi/chi/v5"
	"github.com/jackc/pgx/v5/pgtype"

	"github.com/DarkAbhi/life-backend/internal/mealplan/query"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

var ErrRecipeNotFound = errors.New("mealplan: recipe not found")

const recipePageSize = 24

type RecipeSummary struct {
	ID             string `json:"id"`
	Name           string `json:"name"`
	DishType       string `json:"dish_type"`
	IsReadyToEat   bool   `json:"is_ready_to_eat"`
	ReadyInMinutes *int32 `json:"ready_in_minutes,omitempty"`
}

type RecipePage struct {
	Recipes []RecipeSummary `json:"recipes"`
	HasMore bool            `json:"has_more"`
}

type RecipeDetail struct {
	RecipeSummary
	Description    string          `json:"description"`
	AlternateNames []string        `json:"alternate_names"`
	Ingredients    json.RawMessage `json:"ingredients" swaggertype:"array,object"`
	Steps          json.RawMessage `json:"steps" swaggertype:"array,object"`
	Labels         json.RawMessage `json:"labels" swaggertype:"array,object"`
	Related        json.RawMessage `json:"related" swaggertype:"array,object"`
	Details        json.RawMessage `json:"details" swaggertype:"object"`
}

func (r *Repository) ListCatalogRecipes(ctx context.Context, args query.ListCatalogRecipesParams) ([]query.ListCatalogRecipesRow, error) {
	return r.queries.ListCatalogRecipes(ctx, args)
}

func (r *Repository) GetCatalogRecipe(ctx context.Context, id string) (query.GetCatalogRecipeRow, error) {
	return r.queries.GetCatalogRecipe(ctx, id)
}

func (r *Repository) GetCatalogRecipeName(ctx context.Context, id string) (string, error) {
	return r.queries.GetCatalogRecipeName(ctx, id)
}

func (s *Service) ListRecipes(ctx context.Context, search string, page int) (RecipePage, error) {
	search = strings.TrimSpace(search)
	if utf8.RuneCountInString(search) > 100 || strings.ContainsRune(search, 0) {
		return RecipePage{}, ValidationError{"recipe search must be at most 100 characters"}
	}
	if page < 1 || page > 10000 {
		return RecipePage{}, ValidationError{"recipe page must be between 1 and 10000"}
	}
	rows, err := s.store.ListCatalogRecipes(ctx, query.ListCatalogRecipesParams{
		Search: search, PageSize: recipePageSize + 1, PageOffset: int32((page - 1) * recipePageSize),
	})
	if err != nil {
		return RecipePage{}, fmt.Errorf("list catalog recipes: %w", err)
	}
	result := RecipePage{HasMore: len(rows) > recipePageSize}
	if result.HasMore {
		rows = rows[:recipePageSize]
	}
	result.Recipes = make([]RecipeSummary, 0, len(rows))
	for _, row := range rows {
		result.Recipes = append(result.Recipes, RecipeSummary{
			ID: row.ID, Name: row.Name, DishType: row.DishType, IsReadyToEat: row.IsReadyToEat,
			ReadyInMinutes: recipeMinutes(row.ReadyInMinutes),
		})
	}
	return result, nil
}

func validateRecipeID(id string) (string, error) {
	id = strings.TrimSpace(id)
	if id == "" || len(id) > 100 || strings.ContainsRune(id, 0) {
		return "", ValidationError{"invalid recipe id"}
	}
	return id, nil
}

func (s *Service) GetRecipe(ctx context.Context, id string) (RecipeDetail, error) {
	id, err := validateRecipeID(id)
	if err != nil {
		return RecipeDetail{}, err
	}
	row, err := s.store.GetCatalogRecipe(ctx, id)
	if errors.Is(err, sql.ErrNoRows) {
		return RecipeDetail{}, ErrRecipeNotFound
	}
	if err != nil {
		return RecipeDetail{}, fmt.Errorf("get catalog recipe: %w", err)
	}
	return RecipeDetail{
		RecipeSummary: RecipeSummary{
			ID: row.ID, Name: row.Name, DishType: row.DishType, IsReadyToEat: row.IsReadyToEat,
			ReadyInMinutes: recipeMinutes(row.ReadyInMinutes),
		},
		Description: row.Description, AlternateNames: row.AlternateNames, Details: row.Details,
		Ingredients: row.Ingredients, Steps: row.Steps, Labels: row.Labels, Related: row.Related,
	}, nil
}

func recipeMinutes(value pgtype.Int4) *int32 {
	if !value.Valid {
		return nil
	}
	return &value.Int32
}

func (h *Handler) ListRecipes(w http.ResponseWriter, r *http.Request) {
	if _, ok := h.authenticatedUser(w, r); !ok {
		return
	}
	params := r.URL.Query()
	page := 1
	if raw := params.Get("page"); raw != "" {
		var err error
		page, err = strconv.Atoi(raw)
		if err != nil {
			webutil.BadRequest(w, "invalid recipe page")
			return
		}
	}
	result, err := h.service.ListRecipes(r.Context(), params.Get("q"), page)
	if err != nil {
		writeError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, result)
}

func (h *Handler) GetRecipe(w http.ResponseWriter, r *http.Request) {
	if _, ok := h.authenticatedUser(w, r); !ok {
		return
	}
	result, err := h.service.GetRecipe(r.Context(), chi.URLParam(r, "id"))
	if err != nil {
		writeError(w, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, result)
}
