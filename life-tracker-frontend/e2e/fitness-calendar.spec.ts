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

test("keeps the recurring plan separate from the completed workout type", async ({ page, context }) => {
  await context.addCookies([{ name: "life_session", value: "fitness-workout-schedule", url: "http://localhost:3107" }]);
  await page.goto("/fitness");

  const schedule = page.getByRole("region", { name: "Workout schedule" });
  const friday = schedule.getByRole("button", { name: /Edit Friday schedule/ });
  await friday.click();
  const scheduleEditor = page.getByRole("dialog", { name: "Edit Friday" });
  await scheduleEditor.getByLabel("Workout type").selectOption("push");
  await scheduleEditor.getByLabel("Focus (optional)").fill("Chest and triceps");
  await scheduleEditor.getByRole("button", { name: "Save" }).click();
  await expect(scheduleEditor).toBeHidden();
  await expect(friday).toContainText("Push · Chest and triceps");

  const calendar = page.getByRole("region", { name: "Workout calendar" });
  await expect(calendar.getByText("Scheduled · Push · Chest and triceps")).toBeVisible();
  await calendar.getByRole("link", { name: /View workout for gym visit/ }).click();

  await page.getByRole("button", { name: "Edit workout" }).click();
  const workoutEditor = page.getByRole("dialog", { name: "Edit workout" });
  await expect(workoutEditor.getByLabel("Workout type")).toHaveValue("");
  await workoutEditor.getByLabel("Workout type").selectOption("double_muscle");
  await workoutEditor.getByLabel("Focus (optional)").fill("Back and biceps");
  await workoutEditor.getByRole("button", { name: "Save" }).click();
  await expect(workoutEditor).toBeHidden();
  await expect(page.getByText("Double muscle · Back and biceps", { exact: true })).toBeVisible();

  await page.getByRole("link", { name: "Back to Fitness" }).click();
  await expect(friday).toContainText("Push · Chest and triceps");
  await expect(calendar.getByRole("link", { name: /Double muscle · Back and biceps/ })).toBeVisible();
  await friday.click();
  await scheduleEditor.getByLabel("Workout type").selectOption("rest_day");
  await expect(scheduleEditor.getByLabel("Focus (optional)")).toBeHidden();
  await scheduleEditor.getByRole("button", { name: "Save" }).click();
  await expect(friday).toContainText("Rest day");
  await expect(calendar.getByRole("link", { name: /Double muscle · Back and biceps/ })).toBeVisible();
});
