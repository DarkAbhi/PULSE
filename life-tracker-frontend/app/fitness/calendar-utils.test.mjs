import assert from "node:assert/strict";
import { calendarDays, indiaDateKey, shiftMonth } from "./calendar-utils.mjs";

assert.equal(indiaDateKey(new Date("2026-09-30T18:29:59Z")), "2026-09-30");
assert.equal(indiaDateKey(new Date("2026-09-30T18:30:00Z")), "2026-10-01");
assert.equal(shiftMonth("2026-12", 1), "2027-01");
assert.equal(shiftMonth("2026-01", -1), "2025-12");
const october = calendarDays("2026-10");
assert.equal(october[0], "2026-09-28");
assert.equal(october.at(-1), "2026-11-01");
assert.equal(october.length, 35);
assert.equal(new Set(october).size, october.length);
assert.ok(calendarDays("2028-02").includes("2028-02-29"));
assert.equal(calendarDays("2026-02").includes("2026-02-29"), false);
assert.equal(calendarDays("2026-03").length, 42);
console.log("Calendar date checks passed.");
