import assert from "node:assert/strict";
import { test } from "node:test";
import { stubProperty } from "#tests/helpers/stub-global.mjs";
import { copyApiKey } from "./copy-api-key.ts";

test("copies the API key when Clipboard API is unavailable or denied", async context => {
  let selected = 0;
  stubProperty(context, globalThis, "document", { execCommand: (command) => {
    assert.equal(command, "copy");
    assert.ok(selected > 0);
    return true;
  } });
  stubProperty(context, globalThis.navigator, "clipboard", undefined);

  const input = { select: () => { selected++; } };
  assert.equal(await copyApiKey("secret", input), true);
  Object.defineProperty(globalThis.navigator, "clipboard", {
    configurable: true,
    value: { writeText: async () => { throw new Error("denied"); } },
  });
  assert.equal(await copyApiKey("secret", input), true);
  assert.equal(selected, 2);
});
