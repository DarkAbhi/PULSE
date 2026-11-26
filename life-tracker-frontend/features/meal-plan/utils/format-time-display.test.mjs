import assert from "node:assert/strict";
import { test } from "node:test";
import { formatTimeDisplay } from "./format-time-display.ts";

test("formats 24-hour meal times for display", () => {
  assert.equal(formatTimeDisplay("00:05"), "12:05 AM");
  assert.equal(formatTimeDisplay("09:30:00"), "9:30 AM");
  assert.equal(formatTimeDisplay("12:00"), "12:00 PM");
  assert.equal(formatTimeDisplay("13:17"), "1:17 PM");
  assert.equal(formatTimeDisplay("23:59"), "11:59 PM");
});

test("leaves missing or unparseable times as they are", () => {
  assert.equal(formatTimeDisplay(undefined), "");
  assert.equal(formatTimeDisplay(""), "");
  assert.equal(formatTimeDisplay("noon"), "noon");
});
