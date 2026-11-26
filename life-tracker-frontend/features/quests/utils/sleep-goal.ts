export const sleepGoalHint = "Enter hours or hours:minutes, for example 8 or 7:30.";
export const sleepGoalError = "Use hours or hours:minutes, from 0:01 to 24:00.";

export function parseSleepGoal(value: string): number | null | undefined {
  const input = value.trim();
  if (!input) return null;
  const match = /^(\d{1,2})(?::([0-5]\d))?$/.exec(input);
  if (!match) return undefined;
  const minutes = Number(match[1]) * 60 + Number(match[2] ?? 0);
  return minutes >= 1 && minutes <= 1440 ? minutes : undefined;
}

export function formatSleepGoal(minutes: number | null): string {
  if (minutes === null) return "";
  const hours = Math.floor(minutes / 60);
  const remainder = minutes % 60;
  return remainder ? `${hours}:${String(remainder).padStart(2, "0")}` : String(hours);
}
