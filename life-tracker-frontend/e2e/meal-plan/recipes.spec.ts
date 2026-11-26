import { expect, test } from "@playwright/test";
import { signIn } from "@/e2e/helpers/session";

test("selects and saves a catalog recipe by name alongside custom meals", async ({ context, page }) => {
  await signIn(context, "recipe-test");
  await page.goto("/meal-plan");
  await page.getByRole("button", { name: "Skip setup" }).click();

  await page.getByRole("button", { name: "Add meal to Breakfast" }).click();
  const addMeal = page.getByRole("dialog", { name: "Add meal" });
  await addMeal.getByRole("searchbox", { name: "Search recipes" }).fill("khara");
  await addMeal.getByRole("button", { name: /Ven Pongal/ }).click();
  await expect(addMeal.getByRole("heading", { name: "Ven Pongal", exact: true })).toBeVisible();
  await expect(addMeal.getByRole("searchbox")).toBeHidden();
  await expect(addMeal.getByText("Loading recipe…")).toBeHidden();
  await expect(addMeal.getByRole("heading", { name: "Instructions" })).toBeHidden();
  await addMeal.getByRole("button", { name: "Add meal", exact: true }).click();

  const plannedRecipe = page.getByRole("checkbox", { name: "Eaten: Ven Pongal" });
  await expect(plannedRecipe).toBeVisible();
  await page.reload();
  await expect(plannedRecipe).toBeVisible();
  await expect(page.getByRole("button", { name: "View recipe for Ven Pongal" })).toBeHidden();

  await page.getByRole("button", { name: "Add meal to Breakfast" }).click();
  await addMeal.getByRole("button", { name: "Custom meal" }).click();
  await addMeal.getByRole("textbox", { name: "Meal name" }).fill("Toast");
  await addMeal.getByRole("button", { name: "Add meal", exact: true }).click();
  await expect(page.getByRole("checkbox", { name: "Eaten: Toast" })).toBeVisible();
});
