import { expect, test } from "@playwright/test";

test.beforeEach(async ({ context }) => {
  await context.addCookies([{ name: "life_session", value: "app-bar-navigation", url: "http://localhost:3107" }]);
});

test("screen back links go to their parent from direct entry", async ({ page }) => {
  const routes = [
    ["/garage", "/dashboard"],
    ["/garage/1", "/garage"],
    ["/garage/999", "/garage"],
    ["/garage/1/reminders", "/garage/1?tab=maintenance"],
    ["/garage/1/reminders/engine_oil", "/garage/1/reminders"],
    ["/fitness", "/dashboard"],
    ["/fitness/1", "/fitness"],
    ["/financial-horizon", "/dashboard"],
    ["/meal-plan", "/dashboard"],
    ["/profile", "/dashboard"],
    ["/notifications", "/dashboard"],
  ];

  for (const [path, parent] of routes) {
    await page.goto(path);
    const bar = page.getByRole("navigation", { name: "App navigation" });
    const back = bar.getByRole("link", { name: /^Back to / });
    await expect(back).toHaveAttribute("href", parent);
    await expect(back).toHaveText("");
    await expect(bar.getByRole("link", { name: "Profile" })).toHaveAttribute("href", "/profile");
    await expect(bar.getByRole("button", { name: "Notifications" })).toBeVisible();
  }

  await page.goto("/garage/1/reminders/engine_oil");
  await page.getByRole("link", { name: "Back to reminder settings" }).click();
  await expect(page).toHaveURL("/garage/1/reminders");
  await page.getByRole("link", { name: "Back to Maintenance" }).click();
  await expect(page).toHaveURL("/garage/1?tab=maintenance");
});

test("meal preference editing returns one level to the planner", async ({ page }) => {
  await page.goto("/meal-plan");
  await page.getByRole("button", { name: "Skip setup" }).click();
  await page.getByRole("button", { name: "Meal preferences" }).click();
  await page.getByRole("button", { name: "Back to Meal Plan" }).click();
  await expect(page.getByRole("heading", { name: "Meal Plan", exact: true })).toBeVisible();
});

test("the notification indicator follows unread updates on a narrow screen", async ({ page, context }) => {
  await context.addCookies([{ name: "life_session", value: "app-bar-test", url: "http://localhost:3107" }]);
  await page.setViewportSize({ width: 320, height: 800 });
  await page.goto("/garage");
  const unreadBell = page.getByRole("button", { name: "Notifications, unread updates" });
  await unreadBell.click();

  const panel = page.getByRole("dialog", { name: "Notifications" });
  await expect(panel.getByText("Test reminder")).toBeVisible();
  await expect(panel).toBeInViewport({ ratio: 1 });
  await panel.getByRole("button", { name: "Dismiss Test reminder" }).click();
  await expect(unreadBell).toHaveCount(0);
  await expect(panel.getByText("You're all caught up.")).toBeVisible();
  await panel.getByRole("link", { name: "View all notifications" }).click();
  await expect(page).toHaveURL("/notifications");
  await expect(page.getByRole("button", { name: "Notifications", exact: true })).toBeVisible();
});
