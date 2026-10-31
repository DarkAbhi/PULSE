import test from "node:test";
import assert from "node:assert/strict";
import { CURRENCIES, validCurrency, formatMoney, currencySymbol } from "./currency.mjs";
test("currency selection accepts supported codes and rejects symbols and malformed codes", () => {
 for (const code of ["GBP", "INR", "USD", "JPY", "KWD"]) assert.equal(validCurrency(code), true);
 for (const code of ["", "gbp", "ZZZ", "£", " USD", null]) assert.equal(validCurrency(code), false);
 assert.equal(new Set(CURRENCIES).size, CURRENCIES.length);
});
test("money formatting follows the selected currency's fraction digits", () => {
 assert.match(formatMoney(1234.5, "USD"), /\$1,234.50/);
 assert.match(formatMoney(1234, "JPY"), /¥1,234$/);
 assert.match(formatMoney(1.234, "KWD"), /KWD.*1.234/);
 assert.throws(() => formatMoney(10, "ZZZ"));
});

test("the supported currency choices match the backend and Android", async () => {
 const { readFile } = await import("node:fs/promises");
 const go = await readFile(new URL("../../../go-backend/internal/currency/currency.go", import.meta.url), "utf8");
 const kotlin = await readFile(new URL("../../../pulse-android/core/domain/src/main/kotlin/dev/abhishekan/pulse/core/domain/model/UserCurrency.kt", import.meta.url), "utf8");
 assert.deepEqual(go.match(/const Codes = "([^"]+)"/)[1].split(" "), [...CURRENCIES]);
 assert.deepEqual(kotlin.split('"""')[1].trim().split(/\s+/), [...CURRENCIES]);
});

test("currency symbols follow the selected preference", () => {
 assert.equal(currencySymbol("GBP"), "£");
 assert.equal(currencySymbol("INR"), "₹");
 assert.equal(currencySymbol("EUR"), "€");
 assert.equal(currencySymbol("USD"), "$");
 assert.equal(formatMoney(12.5, "GBP"), "£12.50");
 assert.equal(formatMoney(12.5, "INR"), "₹12.50");
 assert.throws(() => currencySymbol("ZZZ"));
});
