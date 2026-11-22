import { expect, test } from "@playwright/test";

test("fills the same tab cell for every selection", async ({ page, context }) => {
  await context.addCookies([{ name: "life_session", value: "categories-test", url: "http://localhost:3107" }]);
  const navigation = page.getByRole("navigation", { name: "Financial Horizon sections" });

  for (const width of [1440, 390]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto("/financial-horizon");
    const tabs = navigation.getByRole("button");
    await expect(tabs).toHaveCount(7);
    const readWidths = () => tabs.evaluateAll(buttons => buttons.map(button => Math.round(button.getBoundingClientRect().width)));
    const widths = await readWidths();
    expect(new Set(widths).size).toBe(1);
    expect(widths[0]).toBeGreaterThan(100);

    const layout = await navigation.evaluate(nav => ({
      scrolls: nav.scrollHeight > nav.clientHeight,
      columns: new Set(Array.from(nav.children, button => button.getBoundingClientRect().left)).size,
    }));
    expect(layout).toEqual(width === 390 ? { scrolls: true, columns: 1 } : { scrolls: false, columns: 7 });

    for (const name of ["Transactions", "Obligations", "Planner", "Accounts", "Overview"]) {
      const tab = navigation.getByRole("button", { name, exact: true });
      await tab.click();
      await expect(tab).toHaveAttribute("aria-current", "true");
      const hasFill = await tab.evaluate(button => {
        const style = getComputedStyle(button);
        return style.backgroundImage !== "none" || style.backgroundColor !== "rgba(0, 0, 0, 0)";
      });
      expect(hasFill).toBe(true);
      expect(await readWidths()).toEqual(widths);
    }
  }
});

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
