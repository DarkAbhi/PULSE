import { expect, test } from "@playwright/test";
import { signIn } from "@/e2e/helpers/session";

test("dashboard view switches through profile navigation and persists in this browser", async ({ page, context }) => {
  await signIn(context, "dashboard-test");
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.emulateMedia({ reducedMotion: "no-preference" });
  await page.goto("/dashboard");

  const dashboard = page.getByRole("main");
  const hero = page.getByRole("region", { name: "Welcome" });
  await expect(dashboard).toHaveAttribute("data-dashboard-view", "large");
  const largeHero = (await hero.boundingBox())!;
  const largeBottomPadding = await dashboard.evaluate(element => getComputedStyle(element).paddingBottom);
  await page.evaluate(() => { document.documentElement.dataset.dashboardTestDocument = "original"; });

  await page.getByRole("link", { name: "Profile", exact: true }).click();
  const choices = page.getByRole("group", { name: "Dashboard view" });
  await expect(choices.getByRole("radio", { name: "Large" })).toBeChecked();
  await choices.getByRole("radio", { name: "Compact" }).check();
  await page.getByRole("link", { name: "Back to Dashboard" }).click();

  await expect(dashboard).toHaveAttribute("data-dashboard-view", "compact");
  await expect(page.locator("html")).toHaveAttribute("data-dashboard-test-document", "original");
  const compactHero = (await hero.boundingBox())!;
  expect(compactHero.y).toBe(largeHero.y);
  expect(compactHero.height).toBeLessThan(largeHero.height);
  await expect(dashboard).toHaveCSS("padding-bottom", largeBottomPadding);

  const balance = page.getByRole("link", { name: /^Your room to move/ });
  const fitness = page.getByRole("link", { name: /^Fitness/ });
  const garage = page.getByRole("link", { name: /^Garage/ });
  const meals = page.getByRole("link", { name: /^Meal Plan/ });
  await expect(balance).toBeVisible();
  const balanceBox = (await balance.boundingBox())!;
  for (const card of [fitness, garage, meals]) {
    const cardBox = (await card.boundingBox())!;
    expect(cardBox.y).toBe(balanceBox.y);
    expect(cardBox.height).toBe(balanceBox.height);
  }
  await expect(garage.locator("svg line")).toHaveCSS("animation-name", "pulse-sweep");
  await expect(hero.locator('[class*="animate-pulse-breathe"]')).toHaveCSS("animation-name", "pulse-breathe");
  await page.emulateMedia({ reducedMotion: "reduce" });
  await expect(garage.locator("svg line")).toHaveCSS("animation-name", "none");

  // Keep the server-rendered dashboard visible while preventing React from hydrating it.
  const appScript = (url: URL) => url.pathname.startsWith("/_next/") && url.pathname.endsWith(".js");
  await page.route(appScript, route => route.abort());
  await page.reload();
  await expect(dashboard).toHaveAttribute("data-dashboard-view", "initial");
  await expect(hero).toHaveCSS("min-height", "176px");
  await page.unroute(appScript);

  await page.reload();
  await expect(dashboard).toHaveAttribute("data-dashboard-view", "compact");
  await page.setViewportSize({ width: 375, height: 812 });
  expect((await garage.boundingBox())!.y).toBeGreaterThan((await balance.boundingBox())!.y);
  expect(await dashboard.evaluate(element => element.scrollWidth)).toBe(375);

  await page.getByRole("link", { name: "Profile", exact: true }).click();
  await expect(choices.getByRole("radio", { name: "Compact" })).toBeChecked();
  await choices.getByRole("radio", { name: "Large" }).check();
  await page.getByRole("link", { name: "Back to Dashboard" }).click();
  await expect(dashboard).toHaveAttribute("data-dashboard-view", "large");
});

test("an invalid workout response leaves the rest of the dashboard available", async ({ page, context }) => {
  await signIn(context, "dashboard-missing-workout");
  await page.goto("/dashboard");
  await expect(page).toHaveURL("/dashboard");
  await expect(page.getByRole("link", { name: /^Fitness Workout status unavailable\./ })).toBeVisible();
  await expect(page.getByRole("link", { name: /Your room to move.*Keep going/ })).toBeVisible();
  await expect(page.getByRole("link", { name: /^Garage/ })).toHaveAttribute("href", "/garage");
});
