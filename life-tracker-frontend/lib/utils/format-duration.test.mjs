import assert from "node:assert/strict";
import { formatDuration } from "./format-duration.mjs";

assert.equal(formatDuration(null), "Not available");
assert.equal(formatDuration(0), "0 minutes");
assert.equal(formatDuration(30), "Less than a minute");
assert.equal(formatDuration(60), "1 minute");
assert.equal(formatDuration(1800), "30 minutes");
assert.equal(formatDuration(3599.999), "59 minutes");
assert.equal(formatDuration(3600), "1 hour 0 minutes");
assert.equal(formatDuration(3660), "1 hour 1 minute");
assert.equal(formatDuration(7500.125), "2 hours 5 minutes");
