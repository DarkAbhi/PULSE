import { expect, test } from "@playwright/test";
import { signIn } from "@/e2e/helpers/session";

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
  await expect(page.getByRole("link", { name: /^Fitness No workout recorded today\./ })).toBeVisible();
  expect((await context.cookies()).find(({ name }) => name === "life_session")?.httpOnly).toBe(true);

  const response = await context.request.get("/", { maxRedirects: 0 });
  expect(response.status()).toBe(307);
  expect(response.headers().location).toBe("/dashboard");
});

test("an expired session returns to sign-in from the dashboard", async ({ page, context }) => {
  await signIn(context, "expired-session");
  await page.goto("/dashboard");
  await expect(page).toHaveURL("/");
  await expect(page.getByRole("heading", { name: "Welcome back" })).toBeVisible();
});
