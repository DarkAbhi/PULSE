import { expect, test } from "@playwright/test";

test("recent activity appears below fitness in both dashboard sizes", async ({ page, context }) => {
  await context.addCookies([{ name: "life_session", value: "dashboard-activity-test", url: "http://localhost:3107" }]);
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto("/dashboard");

  const dashboard = page.getByRole("main");
  const fitness = page.getByRole("region", { name: "Today at a glance" });
  const activity = page.getByRole("region", { name: "Recent activity" });
  async function expectAlignedSections() {
    const steps = fitness.getByText("Steps", { exact: true });
    await expect(steps).toBeVisible();
    const activityRow = activity.getByRole("listitem").first();
    const [fitnessHeading, activityHeading, fitnessSection, activityCard, stepsLabel, activityLabel, stepsIcon, activityIcon, activityContent] = await Promise.all([
      fitness.getByRole("heading").boundingBox(),
      activity.getByRole("heading").boundingBox(),
      fitness.boundingBox(),
      activity.locator(".pulse-card").boundingBox(),
      steps.boundingBox(),
      activityRow.getByText("Fuel fill-up", { exact: true }).boundingBox(),
      fitness.locator(".pulse-card").first().locator(":scope > span").boundingBox(),
      activityRow.locator(":scope > span").boundingBox(),
      activityRow.locator(":scope > div").boundingBox(),
    ]);
    expect(activityHeading!.x).toBe(fitnessHeading!.x);
    expect(activityCard!.x).toBe(fitnessSection!.x);
    expect(activityCard!.width).toBe(fitnessSection!.width);
    expect(activityIcon!.x).toBe(stepsIcon!.x);
    expect(activityIcon!.width).toBe(stepsIcon!.width);
    expect(activityLabel!.x).toBe(stepsLabel!.x);
    expect(activityCard!.x + activityCard!.width - activityContent!.x - activityContent!.width)
      .toBe(activityIcon!.x - activityCard!.x);
  }

  await expect(dashboard).toHaveAttribute("data-dashboard-view", "large");
  await expect(activity.getByRole("listitem")).toHaveCount(3);
  expect((await activity.boundingBox())!.y).toBeGreaterThan((await fitness.boundingBox())!.y);
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
});

test("a new account sees the empty activity card", async ({ page, context }) => {
  await context.addCookies([{ name: "life_session", value: "empty-activity-test", url: "http://localhost:3107" }]);
  await page.goto("/dashboard");
  await expect(page.getByRole("region", { name: "Recent activity" }).getByText("No recent activity yet.")).toBeVisible();
});
