/** @param {number | null} seconds */
export function formatDuration(seconds) {
  if (seconds == null) return "Not available";
  if (seconds > 0 && seconds < 60) return "Less than a minute";
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;
  const minuteLabel = `${remainingMinutes} ${remainingMinutes === 1 ? "minute" : "minutes"}`;
  return hours ? `${hours} ${hours === 1 ? "hour" : "hours"} ${minuteLabel}` : minuteLabel;
}
