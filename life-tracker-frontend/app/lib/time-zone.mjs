/** Shared calendar/instant conversions; never use the browser's implicit zone. */
export const DEFAULT_TIME_ZONE = "UTC";

/** @param {unknown} zone */
export function validTimeZone(zone) {
  if (typeof zone !== "string" || !zone || zone === "Local" || /^[+-]/.test(zone)) return false;
  try { new Intl.DateTimeFormat("en", { timeZone: zone }); return true; } catch { return false; }
}

// Intl implementations can expose legacy CLDR names; offer the current IANA names too.
const currentNames = {
  "Asia/Calcutta": "Asia/Kolkata", "Asia/Katmandu": "Asia/Kathmandu",
  "Asia/Rangoon": "Asia/Yangon", "Asia/Saigon": "Asia/Ho_Chi_Minh",
  "Europe/Kiev": "Europe/Kyiv", "America/Godthab": "America/Nuuk",
  "Pacific/Enderbury": "Pacific/Kanton", "Pacific/Ponape": "Pacific/Pohnpei",
  "Pacific/Truk": "Pacific/Chuuk", "Atlantic/Faeroe": "Atlantic/Faroe",
};

/** @param {string} selected @param {string[]} [supported] */
export function timeZoneChoices(selected, supported = Intl.supportedValuesOf("timeZone")) {
  const choices = supported.map(zone => {
    const current = currentNames[zone];
    return current && validTimeZone(current) ? current : zone;
  });
  return [...new Set([DEFAULT_TIME_ZONE, selected, ...choices])].sort();
}

/** @param {Date|string|number} value @param {string} timeZone */
function parts(value, timeZone) {
  const date = new Date(value);
  if (!Number.isFinite(date.getTime())) throw new RangeError("Choose a valid date and time.");
  const formatter = new Intl.DateTimeFormat("en-CA", {
    timeZone, calendar: "iso8601", numberingSystem: "latn", hourCycle: "h23",
    year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", second: "2-digit",
  });
  return Object.fromEntries(formatter.formatToParts(date).filter(p => p.type !== "literal").map(p => [p.type, p.value]));
}

/** @param {Date|string|number} value @param {string} timeZone */
export function dateKey(value, timeZone) {
  const p = parts(value, timeZone);
  return `${p.year.padStart(4, "0")}-${p.month}-${p.day}`;
}

/** @param {Date|string|number} value @param {string} timeZone */
export function toDateTimeInput(value, timeZone) {
  const p = parts(value, timeZone);
  return `${dateKey(value, timeZone)}T${p.hour}:${p.minute}`;
}

/** @param {Date|string|number} value @param {string} timeZone @param {Intl.DateTimeFormatOptions} [options] */
export function formatInstant(value, timeZone, options = { dateStyle: "medium", timeStyle: "short" }) {
  return new Intl.DateTimeFormat("en-IN", { ...options, timeZone }).format(new Date(value));
}

/** Date-only values are calendar coordinates, not instants to shift. @param {string} value @param {Intl.DateTimeFormatOptions} [options] */
export function formatCalendarDate(value, options = { dateStyle: "medium" }) {
  return formatInstant(`${value.slice(0, 10)}T00:00:00Z`, "UTC", options);
}

/** @param {string} value */
export function calendarDayNumber(value) {
  return new Date(`${value.slice(0, 10)}T00:00:00Z`).getTime() / 86400000;
}

/** Convert a selected-zone wall time to UTC, rejecting DST gaps.
 * An unchanged input preserves the original instant, including its repeated-hour offset and precision.
 * New ambiguous times use the earlier occurrence, matching Android java.time.
 * @param {string} value @param {string} timeZone @param {string|null} [original]
 */
export function fromDateTimeInput(value, timeZone, original = null) {
  if (original && toDateTimeInput(original, timeZone) === value) return original;
  if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value)) throw new RangeError("Choose a valid date and time.");
  const wall = new Date(`${value}:00Z`).getTime();
  if (!Number.isFinite(wall) || toDateTimeInput(wall, "UTC") !== value) throw new RangeError("Choose a valid date and time.");
  const offsets = new Set();
  // Sample either side of the wall time to include both offsets at a transition.
  for (let hours = -36; hours <= 36; hours += 6) {
    const sample = wall + hours * 3600000;
    const p = parts(sample, timeZone);
    const represented = new Date(`${p.year.padStart(4, "0")}-${p.month}-${p.day}T${p.hour}:${p.minute}:${p.second}Z`).getTime();
    offsets.add(represented - sample);
  }
  const matches = [...offsets].map(offset => wall - offset)
    .filter(candidate => toDateTimeInput(candidate, timeZone) === value).sort((a, b) => a - b);
  if (!matches.length) throw new RangeError("This time does not exist because the clocks move forward. Choose another time.");
  return new Date(matches[0]).toISOString();
}

/** @param {string|null|undefined} raw @param {string} timeZone */
export function statementTransactionInstant(raw, timeZone) {
  if (!raw?.trim()) return new Date().toISOString();
  const value = raw.trim();
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) return fromDateTimeInput(`${value}T12:00`, timeZone);
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d{1,3})?)?(Z|[+-]\d{2}:\d{2})$/i.test(value)) {
    const date = new Date(value);
    if (Number.isFinite(date.getTime())) return date.toISOString();
  }
  const local = value.match(/^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2})(?::(\d{2}))?$/);
  if (local && Number(local[3] ?? 0) < 60) {
    const base = fromDateTimeInput(`${local[1]}T${local[2]}`, timeZone);
    return new Date(new Date(base).getTime() + Number(local[3] ?? 0) * 1000).toISOString();
  }
  throw new RangeError("A statement transaction has an invalid date. Review it before importing.");
}
