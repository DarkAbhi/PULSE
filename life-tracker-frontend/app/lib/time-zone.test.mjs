import test from "node:test";
import assert from "node:assert/strict";
import { DEFAULT_TIME_ZONE, validTimeZone, timeZoneChoices, dateKey, toDateTimeInput, fromDateTimeInput, formatCalendarDate, calendarDayNumber, statementTransactionInstant } from "./time-zone.mjs";

test("UTC default is independent of the host zone; selected zone crosses date/year boundaries", () => {
  assert.equal(DEFAULT_TIME_ZONE, "UTC");
  const instant = "2026-01-01T00:30:00Z";
  assert.equal(dateKey(instant, "UTC"), "2026-01-01");
  assert.equal(dateKey(instant, "America/Los_Angeles"), "2025-12-31");
  assert.equal(toDateTimeInput(instant, "Asia/Kolkata"), "2026-01-01T06:00");
  assert.equal(toDateTimeInput(instant, "Pacific/Kiritimati"), "2026-01-01T14:30");
});

test("wall times convert to UTC and round trip in the account zone", () => {
  for (const zone of ["UTC", "Asia/Kolkata", "America/Los_Angeles", "Pacific/Chatham", "Pacific/Kiritimati"]) {
    const input = "2026-10-28T12:34";
    assert.equal(toDateTimeInput(fromDateTimeInput(input, zone), zone), input);
  }
  assert.equal(fromDateTimeInput("2026-10-28T12:00", "Asia/Kolkata"), "2026-10-28T06:30:00.000Z");
});

test("DST gaps are rejected, including half-hour and skipped-date transitions", () => {
  for (const [zone, input] of [["America/New_York", "2026-03-08T02:30"], ["Australia/Lord_Howe", "2026-10-04T02:15"], ["Pacific/Apia", "2011-12-30T12:00"]]) {
    assert.throws(() => fromDateTimeInput(input, zone), /does not exist/);
  }
});

test("repeated hours use earlier offset for new entries and preserve original offset/precision on edits", () => {
  assert.equal(fromDateTimeInput("2026-11-01T01:30", "America/New_York"), "2026-11-01T05:30:00.000Z");
  const original = "2026-11-01T06:30:19.123456Z";
  assert.equal(fromDateTimeInput("2026-11-01T01:30", "America/New_York", original), original);
  assert.equal(fromDateTimeInput("2026-11-01T01:31", "America/New_York", original), "2026-11-01T05:31:00.000Z");
});

test("invalid input never silently normalizes into a different date", () => {
  for (const input of ["2026-02-30T12:00", "2026-10-28T24:00", "invalid", "2026-10-28", "0000-01-01T00:00"]) {
    assert.throws(() => fromDateTimeInput(input, "UTC"), RangeError);
  }
  for (const zone of ["UPC", "Local", "", "+05:30", null]) assert.equal(validTimeZone(zone), false);
  for (const zone of ["UTC", "Asia/Kolkata", "America/New_York"]) assert.equal(validTimeZone(zone), true);
});

test("calendar-only dates retain their printed day and DST day counts use calendar arithmetic", () => {
  assert.equal(formatCalendarDate("2026-01-01", { year: "numeric", month: "2-digit", day: "2-digit" }), "01/01/2026");
  assert.equal(formatCalendarDate("2026-01-01T00:00:00+14:00", { day: "numeric" }), "1");
  assert.equal(calendarDayNumber("2026-03-09") - calendarDayNumber("2026-03-08"), 1);
});

test("statement dates use the account zone and preserve explicit offsets", () => {
  assert.equal(statementTransactionInstant("2026-10-28", "Asia/Kolkata"), "2026-10-28T06:30:00.000Z");
  assert.equal(statementTransactionInstant("2026-10-28T12:00:45", "Asia/Kolkata"), "2026-10-28T06:30:45.000Z");
  assert.equal(statementTransactionInstant("2026-10-28T12:00:45+05:30", "America/New_York"), "2026-10-28T06:30:45.000Z");
  assert.throws(() => statementTransactionInstant("not a date", "UTC"), RangeError);
});

test("zone choices include UTC, current IANA names and an existing legacy preference", () => {
  const choices = timeZoneChoices("Asia/Calcutta", ["Asia/Calcutta", "America/New_York"]);
  assert.ok(choices.includes("UTC"));
  assert.ok(choices.includes("Asia/Kolkata"));
  assert.ok(choices.includes("Asia/Calcutta"));
  assert.ok(choices.includes("America/New_York"));
});
