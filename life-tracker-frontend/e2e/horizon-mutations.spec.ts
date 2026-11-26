import { expect, test, type Page } from "@playwright/test";

// Each Financial Horizon subdomain owns its dialogs and mutations in a hook; these journeys cover
// the create and delete paths that every hook wires back into the shared horizon summary.
test.beforeEach(async ({ context, page }) => {
  await context.addCookies([{ name: "life_session", value: "horizon-mutations", url: "http://localhost:3107" }]);
  await page.goto("/financial-horizon");
});

async function quickAction(page: Page, name: string) {
  await page.getByRole("button", { name: "Quick Action" }).click();
  await page.getByRole("button", { name: new RegExp(`^${name}`) }).click();
}

async function confirmDelete(page: Page, confirmText: string) {
  const confirmation = page.getByRole("dialog");
  await confirmation.getByRole("button", { name: confirmText }).click();
  await expect(confirmation).toBeHidden();
}

test("adds and deletes a monthly budget", async ({ page }) => {
  await quickAction(page, "Add Monthly Budget");
  const dialog = page.getByRole("dialog");
  await dialog.getByPlaceholder("e.g. Housing & Utilities, Subscriptions, Shopping").fill("Groceries");
  await dialog.getByPlaceholder("0.00").fill("12000");
  await dialog.getByRole("button", { name: "Save Budget" }).click();
  await expect(dialog).toBeHidden();

  await page.getByRole("navigation", { name: "Financial Horizon sections" }).getByRole("button", { name: "Budgets", exact: true }).click();
  await expect(page.getByText("Groceries", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Delete Budget" }).click();
  await confirmDelete(page, "Delete budget");
  await expect(page.getByText("Groceries", { exact: true })).toBeHidden();
});

test("adds and deletes a fixed obligation from the quick action", async ({ page }) => {
  await quickAction(page, "Add Fixed Obligation");
  await expect(page).toHaveURL(/\?tab=fixed$/);
  await page.getByPlaceholder("e.g. Apartment Rent, SIP").fill("Apartment rent");
  await page.getByPlaceholder("0.00").fill("25000");
  await page.getByRole("button", { name: "Save Obligation" }).click();

  await expect(page.getByText("Apartment rent", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Delete obligation" }).click();
  await confirmDelete(page, "Delete obligation");
  await expect(page.getByText("Apartment rent", { exact: true })).toBeHidden();
});

test("adds and deletes a planned purchase", async ({ page }) => {
  await quickAction(page, "Add Planned Purchase");
  const dialog = page.getByRole("dialog");
  await dialog.getByPlaceholder("e.g. Noise Cancelling Headphones").fill("Headphones");
  await dialog.getByPlaceholder("0.00").fill("8000");
  await dialog.getByRole("button", { name: "Add Purchase" }).click();
  await expect(dialog).toBeHidden();

  await page.getByRole("navigation", { name: "Financial Horizon sections" }).getByRole("button", { name: "Planner", exact: true }).click();
  await expect(page.getByText("Headphones", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Delete purchase" }).click();
  await confirmDelete(page, "Delete purchase");
  await expect(page.getByText("Headphones", { exact: true })).toBeHidden();
});

test("adds and deletes a subscription", async ({ page }) => {
  await page.getByRole("navigation", { name: "Financial Horizon sections" }).getByRole("button", { name: "Subscriptions", exact: true }).click();
  await page.getByRole("button", { name: "Add Subscription" }).click();
  const dialog = page.getByRole("dialog");
  await dialog.getByPlaceholder("e.g. Netflix, Spotify, AWS, Gym").fill("Music streaming");
  await dialog.getByPlaceholder("0.00").fill("199");
  await dialog.getByRole("button", { name: "Add Subscription" }).click();
  await expect(dialog).toBeHidden();

  await expect(page.getByText("Music streaming", { exact: true }).first()).toBeVisible();
  await page.getByRole("button", { name: "Delete Subscription" }).click();
  await confirmDelete(page, "Delete subscription");
  await expect(page.getByText("Music streaming", { exact: true })).toHaveCount(0);
});
