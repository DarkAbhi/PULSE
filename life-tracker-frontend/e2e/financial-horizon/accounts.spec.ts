import { expect, test } from "@playwright/test";
import { signIn } from "@/e2e/helpers/session";

test("saves bank and credit card accounts from the dialog footer", async ({ page, context }) => {
  await signIn(context, "accounts-test");
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
