const indiaDateFormatter = new Intl.DateTimeFormat("en-CA", {
  timeZone: "Asia/Kolkata",
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
});

/** @param {Date} date */
export function indiaDateKey(date) {
  const parts = indiaDateFormatter.formatToParts(date);
  const value = (/** @type {string} */ type) => parts.find((part) => part.type === type)?.value;
  return `${value("year")}-${value("month")}-${value("day")}`;
}

/** @param {string} month @param {number} offset */
export function shiftMonth(month, offset) {
  const date = new Date(`${month}-01T00:00:00Z`);
  date.setUTCMonth(date.getUTCMonth() + offset);
  return date.toISOString().slice(0, 7);
}

/** Monday-first weeks, including adjacent-month dates. @param {string} month */
export function calendarDays(month) {
  const first = new Date(`${month}-01T00:00:00Z`);
  const offset = (first.getUTCDay() + 6) % 7;
  const last = new Date(`${shiftMonth(month, 1)}-01T00:00:00Z`);
  last.setUTCDate(0);
  const count = Math.ceil((offset + last.getUTCDate()) / 7) * 7;
  return Array.from({ length: count }, (_, index) => {
    const date = new Date(first);
    date.setUTCDate(1 - offset + index);
    return date.toISOString().slice(0, 10);
  });
}
