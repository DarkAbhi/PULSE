import { expect, test } from "@playwright/test";
import path from "node:path";

test("saves bank and credit card accounts from the dialog footer", async ({ page, context }) => {
  await context.addCookies([{ name: "life_session", value: "accounts-test", url: "http://localhost:3107" }]);
  await page.goto("/financial-horizon");
  await page.getByRole("button", { name: "Accounts", exact: true }).click();

  for (const [name, type, label] of [
    ["Browser savings", "bank_account", "Bank account"],
    ["Browser credit card", "credit_card", "Credit card"],
  ]) {
    await page.getByRole("button", { name: "Add account" }).click();
    const dialog = page.getByRole("dialog", { name: "Add account" });
    await dialog.getByLabel("Account name").fill(name);
    await dialog.getByLabel("Institution").fill("Example Bank");
    await dialog.getByLabel("Account type").selectOption(type);
    if (type === "bank_account") {
      await dialog.getByLabel("Bank account subtype").selectOption("savings");
      const countryCode = dialog.getByLabel("Country code (optional)");
      await expect(countryCode).toHaveAccessibleDescription("Use two letters, such as IN for India.");
      await countryCode.fill("91");
      expect(await countryCode.evaluate((input: HTMLInputElement) => input.validity.patternMismatch)).toBe(true);
      await countryCode.fill("in");
      await expect(countryCode).toHaveValue("IN");
    }
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    await expect(dialog).toBeHidden();
    const account = page.getByRole("listitem").filter({ hasText: name });
    await expect(account).toContainText(`Example Bank · ${label} · INR`);
  }

  await page.reload();
  await page.getByRole("button", { name: "Accounts", exact: true }).click();
  await expect(page.getByText("Browser savings", { exact: true })).toBeVisible();
  await expect(page.getByText("Browser credit card", { exact: true })).toBeVisible();
});

test("reviews a statement before committing selected rows", async ({ page, context }) => {
  await context.addCookies([{ name: "life_session", value: "statement-test", url: "http://localhost:3107" }]);
  const longDescription = "Test market\nTransfer reference: " + "ABC123".repeat(16) + "\nFinal line";
  const batch = {
    id: "test-batch", currency: "INR", status: "ready", revision: 1, ocr_used: false,
    account_confirmation_required: true, reconciliation: { status: "passed" },
    counts: { parsed: 2, new: 1, duplicates: 1, excluded: 0 },
  };
  let committed = false;
  await page.route("**/api/horizon/statement-imports", async route => {
    expect(route.request().method()).toBe("POST");
    expect(route.request().postData()).toContain('filename="statement.pdf"');
    await route.fulfill({ status: 201, json: {
      ...batch, page: 1, page_size: 100, total: 2,
      rows: [
        { id: 1, date: "2026-01-02", description: longDescription, amount: "1020348.00", type: "debit", included: true, duplicate_status: "new", category_id: null, warnings: [] },
        { id: 2, date: "2026-01-04", description: "Test refund", amount: "5.00", type: "credit", included: false, duplicate_status: "skipped", category_id: null, warnings: [] },
      ],
    } });
  });
  await page.route("**/api/horizon/statement-imports/test-batch/rows", async route => {
    expect(route.request().method()).toBe("PATCH");
    expect(route.request().postDataJSON()).toEqual({
      revision: 1, account_confirmed: true, changes: [{ id: 1, description: "Edited market" }],
    });
    await route.fulfill({ json: { ...batch, revision: 2, account_confirmation_required: false } });
  });
  await page.route("**/api/horizon/statement-imports/test-batch/commit", async route => {
    expect(route.request().postDataJSON()).toEqual({ revision: 2 });
    committed = true;
    await route.fulfill({ json: { imported: 1, duplicates_skipped: 1, excluded: 0 } });
  });

  await page.goto("/financial-horizon");
  await page.getByRole("button", { name: "Transactions", exact: true }).click();
  await page.getByRole("button", { name: "Import Statement", exact: true }).click();
  const dialog = page.getByRole("dialog", { name: "Import statement" });
  await dialog.getByLabel("Financial account").selectOption("1");
  await dialog.getByLabel("Statement PDF").setInputFiles(
    path.resolve(process.cwd(), "../tests/statement-import/fixtures/kotak_savings_synthetic.pdf"),
  );
  await dialog.getByRole("button", { name: "Preview statement" }).click();
  await expect(dialog.getByText("Balances reconciled", { exact: false })).toBeVisible();
  await expect(dialog.getByText("Duplicate", { exact: true })).toBeVisible();
  await expect(dialog.getByRole("button", { name: "Confirm import" })).toBeDisabled();
  expect(committed).toBe(false);
  const description = dialog.getByLabel("Description for row 1");
  const rows = dialog.getByRole("list", { name: "Statement rows, page 1" });
  await expect(description).toHaveValue(longDescription);
  for (const width of [320, 390, 768, 1280]) {
    await page.setViewportSize({ width, height: 820 });
    expect(await rows.evaluate(list => list.scrollWidth - list.clientWidth), `row overflow at ${width}px`).toBeLessThanOrEqual(1);
    expect(await description.evaluate(input => input.scrollHeight - input.clientHeight), `description clipping at ${width}px`).toBeLessThanOrEqual(1);
  }
  await description.fill("Edited market");
  await dialog.getByRole("checkbox", { name: "This statement belongs to Test savings." }).check();
  await dialog.getByRole("button", { name: "Confirm import" }).click();
  await expect(dialog.getByText("1 imported · 1 duplicates skipped · 0 excluded")).toBeVisible();
  expect(committed).toBe(true);
});
