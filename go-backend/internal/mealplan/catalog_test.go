package mealplan

import (
	"context"
	"database/sql"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/go-chi/chi/v5"

	"github.com/DarkAbhi/life-backend/internal/mealplan/query"
)

type catalogTestStore struct {
	createPlanStore
	created CreatePlanParams
}

func (s *catalogTestStore) CreatePlan(ctx context.Context, input CreatePlanParams) (CreatePlanRow, error) {
	s.created = input
	row, err := s.createPlanStore.CreatePlan(ctx, input)
	if input.RecipeID.Valid {
		row.RecipeID = &input.RecipeID.String
	}
	return row, err
}

func (*catalogTestStore) GetCatalogRecipeName(_ context.Context, id string) (string, error) {
	if id != "recipe-id" {
		return "", sql.ErrNoRows
	}
	return "Ven Pongal", nil
}

func (s *catalogTestStore) ListCatalogRecipes(_ context.Context, input query.ListCatalogRecipesParams) ([]query.ListCatalogRecipesRow, error) {
	if input.Search != "pongal" || input.PageOffset != recipePageSize || input.PageSize != recipePageSize+1 {
		return nil, ValidationError{"incorrect search or pagination"}
	}
	return make([]query.ListCatalogRecipesRow, recipePageSize+1), nil
}

func TestCatalogRecipeCanBePlannedByID(t *testing.T) {
	for _, tc := range []struct {
		id     string
		status int
	}{
		{"recipe-id", http.StatusCreated},
		{"unknown", http.StatusNotFound},
		{"", http.StatusBadRequest},
	} {
		t.Run(tc.id, func(t *testing.T) {
			repository := &catalogTestStore{}
			handler := NewHandler(NewService(repository), func(*http.Request) (int64, error) { return 1, nil })
			body := `{"date":"2026-11-22","meal_time_id":2,"recipe_id":"` + tc.id + `","name":"Ignored client label"}`
			recorder := httptest.NewRecorder()
			handler.CreatePlan(recorder, httptest.NewRequest(http.MethodPost, "/meal-plans", strings.NewReader(body)))
			if recorder.Code != tc.status {
				t.Fatalf("status = %d, want %d, body = %s", recorder.Code, tc.status, recorder.Body)
			}
			if tc.status == http.StatusCreated {
				if repository.created.Name != "Ven Pongal" || !repository.created.RecipeID.Valid ||
					!strings.Contains(recorder.Body.String(), `"recipe_id":"recipe-id"`) {
					t.Fatalf("catalog identity or name missing: %+v %s", repository.created, recorder.Body)
				}
			} else if repository.writes != 0 {
				t.Fatal("invalid recipe produced a planned meal")
			}
		})
	}
}

func TestRecipePagingAndAuthentication(t *testing.T) {
	service := NewService(&catalogTestStore{})
	page, err := service.ListRecipes(context.Background(), " pongal ", 2)
	if err != nil || len(page.Recipes) != recipePageSize || !page.HasMore {
		t.Fatalf("unexpected page size or continuation: %d %v %v", len(page.Recipes), page.HasMore, err)
	}
	router := chi.NewRouter()
	NewHandler(service, func(*http.Request) (int64, error) { return 0, sql.ErrNoRows }).RegisterRoutes(router)
	for _, path := range []string{"/meal-recipes", "/meal-recipes/recipe-id"} {
		response := httptest.NewRecorder()
		router.ServeHTTP(response, httptest.NewRequest(http.MethodGet, path, nil))
		if response.Code != http.StatusUnauthorized {
			t.Fatalf("%s returned %d", path, response.Code)
		}
	}
}
