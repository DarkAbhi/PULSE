import { expect, test } from "@playwright/test";

test("a failed sign-in preserves the form and a successful sign-in opens the dashboard", async ({ page, context }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { name: "Welcome back" })).toBeVisible();
  await page.getByLabel("Username").fill("dashboard-test");
  await page.getByLabel("Password").fill("wrong-password");
  await page.getByRole("button", { name: "Sign in" }).click();
  await expect(page.getByRole("alert").filter({ hasText: "Invalid username or password" })).toBeVisible();
  await expect(page.getByLabel("Username")).toHaveValue("dashboard-test");
  await expect(page.getByLabel("Password")).toHaveValue("wrong-password");

  await page.getByLabel("Password").fill("fixture-password");
  await page.getByRole("button", { name: "Sign in" }).click();
  await expect(page).toHaveURL("/dashboard");
  await expect(page.getByRole("heading", { name: "Browser test" })).toBeVisible();
  await expect(page.locator('[data-clock="time"]')).toHaveText(/\d/);
  await expect(page.getByRole("heading", { name: "No workout recorded today." })).toBeVisible();
  expect((await context.cookies()).find(({ name }) => name === "life_session")?.httpOnly).toBe(true);

  const response = await context.request.get("/", { maxRedirects: 0 });
  expect(response.status()).toBe(307);
  expect(response.headers().location).toBe("/dashboard");
});

test("an expired session returns to sign-in from the dashboard", async ({ page, context }) => {
  await context.addCookies([{ name: "life_session", value: "expired-session", url: "http://localhost:3107" }]);
  await page.goto("/dashboard");
  await expect(page).toHaveURL("/");
  await expect(page.getByRole("heading", { name: "Welcome back" })).toBeVisible();
});

test("dashboard view switches through profile navigation and persists in this browser", async ({ page, context }) => {
  await context.addCookies([{ name: "life_session", value: "dashboard-test", url: "http://localhost:3107" }]);
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.emulateMedia({ reducedMotion: "no-preference" });
  await page.goto("/dashboard");

  const dashboard = page.getByRole("main");
  const hero = page.getByRole("region", { name: "Welcome" });
  await expect(dashboard).toHaveAttribute("data-dashboard-view", "large");
  const largeHeroHeight = (await hero.boundingBox())!.height;
  await page.evaluate(() => { document.documentElement.dataset.dashboardTestDocument = "original"; });

  await page.getByRole("link", { name: "Profile", exact: true }).click();
  const choices = page.getByRole("group", { name: "Dashboard view" });
  await expect(choices.getByRole("radio", { name: "Large" })).toBeChecked();
  await choices.getByRole("radio", { name: "Compact" }).check();
  await page.getByRole("link", { name: "Back to Dashboard" }).click();

  await expect(dashboard).toHaveAttribute("data-dashboard-view", "compact");
  await expect(page.locator("html")).toHaveAttribute("data-dashboard-test-document", "original");
  expect((await hero.boundingBox())!.height).toBeLessThan(largeHeroHeight);

  const balance = page.getByRole("link", { name: /^Your room to move/ });
  const fitness = page.getByRole("article", { name: "Fitness today" });
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
  await context.addCookies([{ name: "life_session", value: "dashboard-missing-workout", url: "http://localhost:3107" }]);
  await page.goto("/dashboard");
  await expect(page).toHaveURL("/dashboard");
  await expect(page.getByRole("heading", { name: "Workout status unavailable." })).toBeVisible();
  await expect(page.getByRole("link", { name: /Your room to move.*Keep going/ })).toBeVisible();
  await expect(page.getByRole("link", { name: /^Garage/ })).toHaveAttribute("href", "/garage");
});
