export function parseYMD(dateStr: string): Date {
  return new Date(`${dateStr}T00:00:00Z`);
}

export function formatYMD(d: Date): string {
  const year = d.getUTCFullYear();
  const month = String(d.getUTCMonth() + 1).padStart(2, "0");
  const day = String(d.getUTCDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function getMonday(d: Date): Date {
  const date = new Date(d);
  const day = date.getUTCDay();
  const diff = (day === 0 ? -6 : 1) - day;
  date.setUTCDate(date.getUTCDate() + diff);
  date.setUTCHours(0, 0, 0, 0);
  return date;
}

export function addDays(d: Date, days: number): Date {
  const res = new Date(d);
  res.setUTCDate(res.getUTCDate() + days);
  return res;
}

export function mealWeekBounds(date: string) {
  const start = getMonday(parseYMD(date));
  return { start: formatYMD(start), end: formatYMD(addDays(start, 6)) };
}
