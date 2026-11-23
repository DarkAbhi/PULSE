import { expect, test } from "@playwright/test";

test("logs another gym visit on the same day and keeps both workout links", async ({ page, context }) => {
  await context.addCookies([{ name: "life_session", value: "fitness-repeat-visit", url: "http://localhost:3107" }]);
  await page.goto("/fitness");

  const calendar = page.getByRole("region", { name: "Workout calendar" });
  const logVisit = calendar.getByRole("button", { name: "Log Visit", exact: true });
  const morningVisit = calendar.getByRole("link", { name: "View workout for gym visit at 7:00 AM for 1 h 12 min", exact: true });
  await expect(morningVisit).toHaveAttribute("href", "/fitness/1");
  await expect(morningVisit).toHaveText("View workout");
  await expect(calendar.getByText("Gym visit at 7:00 AM for 1 h 12 min", { exact: true })).toBeVisible();

  await logVisit.click();
  const dialog = page.getByRole("dialog", { name: "Log gym visit?" });
  await dialog.getByRole("button", { name: "Log gym visit", exact: true }).click();
  await expect(dialog).toBeHidden();
  await expect(calendar.getByRole("link", { name: "View workout for gym visit at 5:00 PM", exact: true })).toHaveAttribute("href", "/fitness/2");
  await expect(morningVisit).toHaveAttribute("href", "/fitness/1");
  await expect(logVisit).toBeVisible();

  await calendar.getByRole("button", { name: /\b3 January 2026, no workouts/ }).click();
  await expect(logVisit).toBeHidden();
});
