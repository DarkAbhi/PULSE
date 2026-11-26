import { expect, test } from "@playwright/test";
import { signIn } from "@/e2e/helpers/session";

test("reviews a statement before committing selected rows", async ({ page, context }) => {
  await signIn(context, "categories-test");
  const longDescription = "Test market\nTransfer reference: " + "ABC123".repeat(16) + "\nFinal line";
  const originalNarration = "UPI/DR/104332516949/ICCL ZER/HDFC/zerodha/\n" + "opaqueReference".repeat(12);
  const batch = {
    id: "test-batch", currency: "INR", status: "ready", revision: 1, ocr_used: false,
    account_confirmation_required: true, reconciliation: { status: "passed" },
    counts: { parsed: 3, new: 2, duplicates: 1, excluded: 0 },
  };
  let committed = false;
  await page.route("**/api/horizon/statement-imports", async route => {
    expect(route.request().method()).toBe("POST");
    expect(route.request().postData()).toContain('filename="statement.pdf"');
    await route.fulfill({ status: 201, json: {
      ...batch, page: 1, page_size: 100, total: 3,
      rows: [
        { id: 1, date: "2026-01-02", description: longDescription, amount: "1020348.00", type: "debit", included: true, duplicate_status: "new", category_id: null, warnings: [] },
        { id: 2, date: "2026-01-03", description: "Zerodha payment", original_narration: originalNarration, amount: "3000.00", type: "debit", included: true, duplicate_status: "new", category_id: 4, warnings: [] },
        { id: 3, date: "2026-01-04", description: "Test refund", amount: "5.00", type: "credit", included: false, duplicate_status: "skipped", category_id: null, warnings: [] },
      ],
    } });
  });
  await page.route("**/api/horizon/statement-imports/test-batch/rows", async route => {
    expect(route.request().method()).toBe("PATCH");
    expect(route.request().postDataJSON()).toEqual({
      revision: 1, account_confirmed: true, changes: [{ id: 1, description: "Edited market" }, { id: 2, description: "Broker payment", category_id: 2 }],
    });
    await route.fulfill({ json: { ...batch, revision: 2, account_confirmation_required: false } });
  });
  await page.route("**/api/horizon/statement-imports/test-batch/commit", async route => {
    expect(route.request().postDataJSON()).toEqual({ revision: 2 });
    committed = true;
    await route.fulfill({ json: { imported: 2, duplicates_skipped: 1, excluded: 0 } });
  });

  await page.goto("/financial-horizon");
  await page.getByRole("button", { name: "Transactions", exact: true }).click();
  await page.getByRole("button", { name: "Import Statement", exact: true }).click();
  const dialog = page.getByRole("dialog", { name: "Import statement" });
  await dialog.getByLabel("Financial account").selectOption("1");
  await dialog.getByLabel("Statement PDF").setInputFiles({
    name: "mock-statement.pdf",
    mimeType: "application/pdf",
    buffer: Buffer.from("%PDF-1.4\n%%EOF"),
  });
  await dialog.getByRole("button", { name: "Preview statement" }).click();
  await expect(dialog.getByText("Balances reconciled", { exact: false })).toBeVisible();
  await expect(dialog.getByText("Duplicate", { exact: true })).toBeVisible();
  await expect(dialog.getByRole("button", { name: "Confirm import" })).toBeDisabled();
  expect(committed).toBe(false);
  const description = dialog.getByLabel("Description for row 1");
  const rows = dialog.getByRole("list", { name: "Statement rows, page 1" });
  await expect(description).toHaveValue(longDescription);
  await expect(dialog.getByLabel("Category for row 1").locator("option:checked")).toHaveText("Uncategorized");
  await expect(dialog.getByLabel("Description for row 2")).toHaveValue("Zerodha payment");
  const suggestedCategory = dialog.getByLabel("Category for row 2");
  await expect(suggestedCategory.locator("option:checked")).toHaveText("Test category / others");
  const narrationToggle = dialog.getByLabel("Original statement narration for row 2");
  const narration = dialog.getByText(originalNarration, { exact: true });
  await expect(dialog.locator("details")).toHaveCount(1);
  await expect(narration).toBeHidden();
  await narrationToggle.focus();
  await page.keyboard.press("Enter");
  await expect(narration).toBeVisible();
  await expect(narration).toHaveJSProperty("isContentEditable", false);
  for (const width of [320, 390, 768, 1280]) {
    await page.setViewportSize({ width, height: 820 });
    expect(await rows.evaluate(list => list.scrollWidth - list.clientWidth), `row overflow at ${width}px`).toBeLessThanOrEqual(1);
    expect(await description.evaluate(input => input.scrollHeight - input.clientHeight), `description clipping at ${width}px`).toBeLessThanOrEqual(1);
  }
  await description.fill("Edited market");
  await dialog.getByLabel("Description for row 2").fill("Broker payment");
  await expect(narration).toHaveText(originalNarration);
  await suggestedCategory.selectOption({ label: "Test category / Test detail" });
  await dialog.getByRole("checkbox", { name: "This statement belongs to Test savings." }).check();
  await dialog.getByRole("button", { name: "Confirm import" }).click();
  await expect(dialog.getByText("2 imported · 1 duplicates skipped · 0 excluded")).toBeVisible();
  expect(committed).toBe(true);
});

test("shows the original narration when editing an imported transaction", async ({ page, context }) => {
  await signIn(context, "categories-test");
  await page.goto("/financial-horizon");
  await page.getByRole("button", { name: "Transactions", exact: true }).click();
  await page.getByTitle("Edit transaction").nth(1).click();
  const dialog = page.getByRole("dialog", { name: "Edit Transaction", exact: true });
  const narration = dialog.getByText("UPI/DR/104332516949/ICCL ZER/HDFC/zerodha/", { exact: true });
  await expect(narration).toBeHidden();
  await dialog.getByText("Original statement narration", { exact: true }).click();
  await expect(narration).toBeVisible();
  await expect(narration).toHaveJSProperty("isContentEditable", false);
  const title = dialog.getByPlaceholder("e.g. Netflix Monthly Payment, Spotify");
  await expect(title).toHaveValue("Test child expense");
  await title.fill("Updated display name");
  await expect(narration).toHaveText("UPI/DR/104332516949/ICCL ZER/HDFC/zerodha/");
  await dialog.getByRole("button", { name: "Cancel", exact: true }).click();
});
