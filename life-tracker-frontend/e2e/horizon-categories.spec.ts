import { expect, test } from "@playwright/test";

test("shows category paths and filters by parent or subcategory from the server", async ({ page, context }) => {
  await context.addCookies([{ name: "life_session", value: "categories-test", url: "http://localhost:3107" }]);
  await page.goto("/financial-horizon");
  await page.getByRole("button", { name: "Transactions", exact: true }).click();

  const parent = page.getByText("Test parent expense", { exact: true });
  const child = page.getByText("Test child expense", { exact: true });
  const standalone = page.getByText("Test standalone expense", { exact: true });
  await expect(standalone).toBeVisible();

  const filter = page.getByLabel("Filter by category");
  await filter.selectOption({ label: "Test category" });
  await expect(standalone).toBeHidden();
  await expect(parent).toBeVisible();
  await expect(child).toBeVisible();

  await filter.selectOption({ label: "Test category / Test detail" });
  await expect(parent).toBeHidden();
  await expect(child).toBeVisible();

  await page.getByRole("button", { name: "Add Category", exact: true }).click();
  const dialog = page.getByRole("dialog", { name: "Add Custom Category" });
  const parentPicker = dialog.getByLabel("Parent Category");
  await expect(parentPicker.locator("option")).toHaveText(["None (Top-level category)", "Test category"]);
});
