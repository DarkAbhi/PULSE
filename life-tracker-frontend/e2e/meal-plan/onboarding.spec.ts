import { test, expect, type Page } from "@playwright/test";
import { signIn } from "@/e2e/helpers/session";
import { API_URL, APP_URL } from "@/e2e/helpers/environment";

async function fixture(page: Page, user: string) { return (await page.request.get(`${API_URL}/__test/state?user=${user}`)).json(); }
async function fail(page: Page, user: string, mode: string) { await page.request.post(`${API_URL}/__test/fail?user=${user}`, { data: { mode } }); }
async function completeQuestions(page: Page) {
  await page.getByRole("radio", { name: "Feel full and satisfied", exact: true }).check();
  await page.getByRole("checkbox", { name: "I'm too tired to figure out meals", exact: true }).check();
  await page.getByRole("radio", { name: "Vegetarian", exact: true }).check();
  await page.getByRole("checkbox", { name: "Peanuts", exact: true }).check();
  await page.getByRole("switch", { name: "Breakfast", exact: true }).uncheck();
  await expect(page.getByRole("switch", { name: "Evening Snacks", exact: true })).toBeChecked();
  await expect(page.getByRole("group", { name: "Usual time for Breakfast" })).toHaveCount(0);
  await page.getByLabel("Lunch minutes", { exact: true }).selectOption("17");
  await page.getByRole("switch", { name: "Lunch", exact: true }).uncheck();
  await page.getByRole("switch", { name: "Lunch", exact: true }).check();
  await expect(page.getByLabel("Lunch minutes", { exact: true })).toHaveValue("17");
  await page.getByLabel("Lunch hour", { exact: true }).selectOption("1");
  await expect(page.getByLabel("Evening Snacks minutes", { exact: true })).toHaveValue("30");
  const summary = page.getByRole("complementary", { name: "Your plan so far" });
  await expect(summary.getByText("1:17 PM")).toBeVisible();
}

test("partial answers, refresh, page close and a fresh browser do not dismiss onboarding", async ({ page, context, browser }) => {
  const user = "unfinished"; await signIn(context, user); await page.goto("/meal-plan");
  await expect(page.getByRole("heading", { name: "What's your goal?" })).toBeVisible();
  const goalHeading = page.getByRole("heading", { name: "What's your goal?" });
  await page.getByRole("button", { name: "Save preferences", exact: true }).click();
  await expect(page.getByRole("main").getByRole("alert").filter({ hasText: "Choose a goal" })).toBeVisible();
  await expect(goalHeading).toBeFocused();
  await page.getByRole("radio", { name: "Find more calm around food", exact: true }).check();
  expect((await fixture(page, user)).writes).toBe(0);
  await page.reload(); await expect(page.getByRole("heading", { name: "What's your goal?" })).toBeVisible();
  await page.close();
  const fresh = await browser.newContext(); await signIn(fresh, user); const returned = await fresh.newPage(); await returned.goto(`${APP_URL}/meal-plan`);
  await expect(returned.getByRole("heading", { name: "What's your goal?" })).toBeVisible();
  expect((await fixture(returned, user)).state.status).toBe("pending"); await fresh.close();
});

test("save round-trips answers, suppresses onboarding and supports later edits", async ({ page, context, browser }) => {
  const user = "completed"; await signIn(context, user); await page.goto("/meal-plan"); await completeQuestions(page);
  expect((await fixture(page, user)).writes).toBe(0);
  await expect(page.getByRole("radio", { name: "Feel full and satisfied", exact: true })).toBeChecked();
  await page.getByRole("button", { name: "Save preferences", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Meal Plan", exact: true })).toBeVisible();
  const saved = (await fixture(page, user)).state;
  expect(saved.preferences).toEqual({ goal: "feel_satisfied", barriers: ["too_tired"], dietary_pattern: "vegetarian", food_avoidances: ["peanuts"], planning_slots_configured: true, meals: [{ kind: "lunch", usual_time: "13:17" }, { kind: "evening_snacks", usual_time: "17:30" }, { kind: "dinner", usual_time: "19:30" }] });
  await expect(page.getByText("Your usual time: 1:17 PM")).toBeVisible();
  await page.getByRole("button", { name: "Add meal", exact: true }).click();
  await expect(page.getByRole("option", { name: /Breakfast/ })).toHaveCount(0);
  await expect(page.getByRole("option", { name: /Evening Snacks/ })).toHaveCount(1);
  await page.getByRole("button", { name: "Cancel", exact: true }).click();
  const fresh = await browser.newContext(); await signIn(fresh, user); const returned = await fresh.newPage(); await returned.goto(`${APP_URL}/meal-plan`);
  await expect(returned.getByRole("heading", { name: "Meal Plan", exact: true })).toBeVisible();
  await returned.getByRole("button", { name: "Meal preferences", exact: true }).click();
  await expect(returned.getByRole("radio", { name: "Feel full and satisfied", exact: true })).toBeChecked();
  await returned.getByRole("radio", { name: "Find more calm around food", exact: true }).check();
  await returned.getByRole("button", { name: "Save preferences", exact: true }).click();
  await expect(returned.getByRole("heading", { name: "Meal Plan", exact: true })).toBeVisible();
  expect((await fixture(returned, user)).state.preferences.goal).toBe("food_calm"); await fresh.close();
});

test("custom slots can be created and removed from the meal switches", async ({ page, context }) => {
  await signIn(context, "custom-slots");
  await page.goto("/meal-plan");
  await page.getByRole("button", { name: "Skip setup" }).click();
  await page.getByRole("button", { name: "Meal preferences" }).click();
  await page.getByRole("radio", { name: "Feel full and satisfied", exact: true }).check();
  await page.getByRole("radio", { name: "Vegetarian", exact: true }).check();
  await page.getByRole("switch", { name: "Lunch", exact: true }).uncheck();
  await page.getByRole("button", { name: "Add custom meal time" }).click();
  await page.getByLabel("Meal time name").fill("After training");
  await page.getByRole("button", { name: "Create meal time" }).click();
  await expect(page.getByRole("group", { name: "When do you usually eat these meals?" }).getByText("After training", { exact: true })).toBeVisible();
  await expect(page.getByRole("switch", { name: "Lunch", exact: true })).not.toBeChecked();
  await page.getByRole("button", { name: "Delete After training" }).click();
  await page.getByRole("button", { name: "Remove meal time", exact: true }).click();
  await expect(page.getByRole("group", { name: "When do you usually eat these meals?" }).getByText("After training", { exact: true })).toHaveCount(0);
});

test("skip persists across sessions; account switching does not share dismissal", async ({ page, context, browser }) => {
  const user = "skipped"; await signIn(context, user); await page.goto("/meal-plan");
  await page.getByRole("button", { name: "Skip setup", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Meal Plan", exact: true })).toBeVisible();
  expect((await fixture(page, user)).state.preferences).toBeNull();
  await page.reload(); await expect(page.getByRole("heading", { name: "Meal Plan", exact: true })).toBeVisible();
  const fresh = await browser.newContext(); await signIn(fresh, user); const other = await fresh.newPage(); await other.goto(`${APP_URL}/meal-plan`);
  await expect(other.getByRole("heading", { name: "Meal Plan", exact: true })).toBeVisible();
  await signIn(fresh, "different-account"); await other.reload(); await expect(other.getByRole("heading", { name: "What's your goal?" })).toBeVisible(); await fresh.close();
});

test("read failures retry; failed skip/save retain choices; lost responses reconcile", async ({ page, context }) => {
  const user = "failures"; await signIn(context, user); await fail(page, user, "read"); await page.goto("/meal-plan");
  await expect(page.getByRole("button", { name: "Try again" })).toBeVisible();
  await fail(page, user, ""); await page.getByRole("button", { name: "Try again" }).click();
  await expect(page.getByRole("heading", { name: "What's your goal?" })).toBeVisible();
  await fail(page, user, "write"); await page.getByRole("button", { name: "Skip setup", exact: true }).click();
  await expect(page.getByRole("main").getByRole("alert")).toContainText("Test connection interrupted"); expect((await fixture(page,user)).state.status).toBe("pending");
  await completeQuestions(page); await page.getByRole("button", { name: "Save preferences", exact: true }).click();
  await expect(page.getByRole("main").getByRole("alert")).toContainText("Test connection interrupted");
  await expect(page.getByRole("checkbox", { name: "Peanuts", exact: true })).toBeChecked();
  await fail(page, user, "lost-response"); await page.getByRole("button", { name: "Save preferences", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Meal Plan", exact: true })).toBeVisible(); expect((await fixture(page,user)).writes).toBe(1);
});

test("another tab's terminal decision is respected", async ({ page, context }) => {
  await signIn(context, "two-tabs"); await page.goto("/meal-plan"); const other = await context.newPage(); await other.goto("/meal-plan");
  await page.getByRole("button", { name: "Skip setup", exact: true }).click(); await completeQuestions(other);
  await other.getByRole("button", { name: "Save preferences", exact: true }).click();
  await expect(other.getByRole("heading", { name: "Meal Plan", exact: true })).toBeVisible();
  await expect(other.getByRole("status").filter({ hasText: "another tab" })).toBeVisible();
});

test("mobile and dark layouts keep long choices and actions reachable", async ({ page, context }) => {
  await signIn(context, "mobile"); await page.setViewportSize({ width: 1440, height: 1000 }); await page.goto("/meal-plan");
  await page.screenshot({ path: "test-results/meal-onboarding-desktop.png", fullPage: true });
  await page.setViewportSize({ width: 768, height: 1024 });
  await page.screenshot({ path: "test-results/meal-onboarding-tablet.png", fullPage: true });
  await page.setViewportSize({ width: 320, height: 700 });
  await expect(page.getByRole("heading", { name: "Meal preferences" })).toBeFocused();
  await expect(page.getByRole("heading", { level: 2 })).toHaveCount(6);
  await expect(page.getByRole("button", { name: "Save preferences", exact: true })).toBeInViewport();
  await page.screenshot({ path: "test-results/meal-onboarding-phone.png", fullPage: true });
  await page.getByRole("radio", { name: "Feel full and satisfied", exact: true }).check();
  await page.getByRole("radio", { name: "Vegan", exact: true }).check();
  await page.getByRole("checkbox", { name: "Alcohol", exact: true }).check();
  await page.getByRole("button", { name: "Clear all", exact: true }).click();
  await expect(page.getByRole("checkbox", { name: "Alcohol", exact: true })).not.toBeChecked();
  await page.getByRole("button", { name: "Dinner AM", exact: true }).click();
  await expect(page.getByRole("button", { name: "Dinner AM", exact: true })).toHaveAttribute("aria-pressed", "true");
  await expect(page.getByRole("button", { name: "Save preferences", exact: true })).toBeInViewport();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  await page.screenshot({ path: "test-results/meal-onboarding-avoidances-phone.png", fullPage: true });
  await page.emulateMedia({ colorScheme: "dark", reducedMotion: "reduce" }); await page.setViewportSize({ width: 1440, height: 1000 });
  await page.screenshot({ path: "test-results/meal-onboarding-desktop-dark.png", fullPage: true });
  await expect(page.getByRole("button", { name: "Save preferences", exact: true })).toBeInViewport();
});

