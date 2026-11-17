import { expect, test } from "@playwright/test";

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
    await dialog.getByLabel("Account type", { exact: true }).selectOption(type);
    await dialog.getByLabel(type === "bank_account" ? "Account holder name" : "Cardholder name").fill("A Person");
    await expect(dialog.getByLabel("Country code", { exact: false })).toHaveCount(0);
    await expect(dialog.getByLabel("Currency", { exact: true })).toHaveCount(0);
    if (type === "bank_account") {
      await dialog.getByLabel("Bank account type", { exact: true }).selectOption("savings");
      await dialog.getByLabel("Account holder email").fill("person@example.com");
      await dialog.getByLabel("Account number (optional)").fill("0012345678");
    } else {
      await dialog.getByLabel("Card type", { exact: true }).fill("Millennia");
      await dialog.getByLabel("Card network").selectOption("rupay");
      await dialog.getByLabel("Last four digits").fill("0123");
      await dialog.getByLabel("Credit limit", { exact: false }).fill("200000.50");
      await dialog.getByLabel("Expected statement day", { exact: false }).fill("31");
    }
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    await expect(dialog).toBeHidden();
    const account = page.getByRole("listitem").filter({ hasText: name });
    await expect(account).toContainText(`Example Bank · ${label}`);
  }

  await page.reload();
  await page.getByRole("button", { name: "Accounts", exact: true }).click();
  await expect(page.getByText("Browser savings", { exact: true })).toBeVisible();
  await expect(page.getByText("Browser credit card", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Edit Browser savings", exact: true }).click();
  const bankEditor = page.getByRole("dialog", { name: "Edit account" });
  await expect(bankEditor.getByLabel("Account number (optional)")).toHaveValue("0012345678");
  await expect(bankEditor.getByLabel("Account holder email")).toHaveValue("person@example.com");
  await bankEditor.getByLabel("Bank account type", { exact: true }).selectOption("current");
  await bankEditor.getByRole("button", { name: "Save", exact: true }).click();
  await expect(bankEditor).toBeHidden();
  await page.getByRole("button", { name: "Edit Browser credit card", exact: true }).click();
  const cardEditor = page.getByRole("dialog", { name: "Edit account" });
  await expect(cardEditor.getByLabel("Card type", { exact: true })).toHaveValue("Millennia");
  await expect(cardEditor.getByLabel("Card network")).toHaveValue("rupay");
  await expect(cardEditor.getByLabel("Credit limit", { exact: false })).toHaveValue("200000.5");
  await expect(cardEditor.getByLabel("Expected statement day", { exact: false })).toHaveValue("31");
  await cardEditor.getByRole("button", { name: "Cancel", exact: true }).click();
  await page.getByRole("button", { name: "Archive Browser credit card", exact: true }).click();
  await expect(page.getByRole("button", { name: "Restore Browser credit card", exact: true })).toBeVisible();
});

test("reviews a statement before committing selected rows", async ({ page, context }) => {
  await context.addCookies([{ name: "life_session", value: "categories-test", url: "http://localhost:3107" }]);
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
  await context.addCookies([{ name: "life_session", value: "categories-test", url: "http://localhost:3107" }]);
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
