package apidoc

// @Summary Browse and search the recipe catalog
// @Description Searches names, aliases, dish types, categories, tags, collections and meal times.
// @Tags meal plans
// @Produce json
// @Param q query string false "Literal search text, at most 100 characters"
// @Param page query int false "Page number, starting at 1"
// @Security SessionBearer
// @Success 200 {object} mealplan.RecipePage
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/meal-recipes [get]
func listRecipes() {}

// @Summary Get a recipe and its ingredients, instructions and classifications
// @Tags meal plans
// @Produce json
// @Param id path string true "Catalog recipe ID"
// @Security SessionBearer
// @Success 200 {object} mealplan.RecipeDetail
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {object} Error
// @Failure 500 {object} Error
// @Router /api/meal-recipes/{id} [get]
func getRecipe() {}
