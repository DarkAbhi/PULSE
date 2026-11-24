import { expect, test } from "@playwright/test";

test("the activity sections follow the reference in both dashboard sizes", async ({ page, context }) => {
  await context.addCookies([{ name: "life_session", value: "dashboard-activity-test", url: "http://localhost:3107" }]);
  await page.emulateMedia({ colorScheme: "light" });
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto("/dashboard");

  const dashboard = page.getByRole("main");
  const fitness = page.getByRole("region", { name: "Today at a glance" });
  const activity = page.getByRole("region", { name: "Recent activity" });
  async function expectAlignedSections() {
    const steps = fitness.getByText("Steps", { exact: true });
    await expect(steps).toBeVisible();
    const activityRow = activity.getByRole("listitem").first();
    const [fitnessHeading, activityHeading, fitnessSection, activityCard, stepsLabel, activityLabel, stepsIcon, activityIcon] = await Promise.all([
      fitness.getByRole("heading").boundingBox(),
      activity.getByRole("heading").boundingBox(),
      fitness.boundingBox(),
      activity.locator(".pulse-card").boundingBox(),
      steps.boundingBox(),
      activityRow.getByText("Fuel fill-up", { exact: true }).boundingBox(),
      fitness.locator(".pulse-card").first().locator(":scope > span").boundingBox(),
      activityRow.locator(":scope > span").boundingBox(),
    ]);
    expect(activityHeading!.x).toBe(fitnessHeading!.x);
    expect(activityCard!.x).toBe(fitnessSection!.x);
    expect(activityCard!.width).toBe(fitnessSection!.width);
    expect(stepsIcon!.width).toBe(56);
    expect(activityIcon!.width).toBe(48);
    expect(stepsIcon!.x - fitnessSection!.x).toBe(20);
    expect(activityIcon!.x - activityCard!.x).toBe(24);
    expect(stepsLabel!.x - stepsIcon!.x - stepsIcon!.width).toBe(16);
    expect(activityLabel!.x - activityIcon!.x - activityIcon!.width).toBe(16);
  }

  await expect(dashboard).toHaveAttribute("data-dashboard-view", "large");
  await expect(activity.getByRole("listitem")).toHaveCount(3);
  const quests = page.getByRole("region", { name: "Quest activity" });
  expect((await quests.boundingBox())!.y).toBeGreaterThan((await fitness.boundingBox())!.y);
  expect((await activity.boundingBox())!.y).toBeGreaterThan((await quests.boundingBox())!.y);
  await expect(fitness.getByRole("img", { name: "80 percent of the 5,000 step goal" })).toBeVisible();
  await expect(fitness.getByText("1 of 3 rings closed")).toBeVisible();
  await expectAlignedSections();
  await expect(activity.getByText("Himalayan 411 · 12.4 L · ₹1,478.00")).toBeVisible();
  await expect(activity.getByText("Paneer burrito bowl · Lunch")).toBeVisible();
  await expect(activity.getByText("HDFC Millennia")).toBeVisible();
  await expect(activity.getByRole("link")).toHaveCount(0);

  await page.getByRole("link", { name: "Profile", exact: true }).click();
  await page.getByRole("radio", { name: "Compact" }).check();
  await page.getByRole("link", { name: "Back to Dashboard" }).click();
  await expect(dashboard).toHaveAttribute("data-dashboard-view", "compact");
  await expect(activity.getByRole("listitem")).toHaveCount(3);
  await expectAlignedSections();
  await page.setViewportSize({ width: 375, height: 812 });
  await expectAlignedSections();
  expect(await activity.evaluate(element => element.scrollWidth)).toBeLessThanOrEqual(375);
  await quests.scrollIntoViewIfNeeded();
  const today = quests.getByRole("button", { name: "Tuesday, January 6, 2026 · 1 quest completed" });
  await expect(today).toBeInViewport();
  await expect(today).toHaveAttribute("aria-pressed", "true");
  await today.focus();
  await today.press("ArrowUp");
  const yesterday = quests.getByRole("button", { name: "Monday, January 5, 2026 · 1 quest completed" });
  await expect(yesterday).toBeFocused();
  await expect(quests.getByText("Monday, January 5, 2026", { exact: true })).toBeVisible();

  const colors = ["rgb(111, 221, 242)", "rgb(76, 107, 255)", "rgb(139, 63, 224)", "rgb(212, 31, 176)"];
  for (const [day, level] of [3, 2, 2, 1, 0, 0].entries()) {
    await expect(quests.locator(`[data-date="2026-01-0${day + 1}"]`)).toHaveCSS("background-color", colors[level]);
  }
});

test("a new account sees the empty activity card", async ({ page, context }) => {
  await context.addCookies([{ name: "life_session", value: "empty-activity-test", url: "http://localhost:3107" }]);
  await page.goto("/dashboard");
  await expect(page.getByRole("region", { name: "Recent activity" }).getByText("No recent activity yet.")).toBeVisible();
});
