/**
 * @param {number | null} seconds
 * @param {"long" | "short"} [style]
 */
export function formatDuration(seconds, style = "long") {
  if (seconds == null) return "Not available";
  if (seconds > 0 && seconds < 60) return style === "short" ? "<1 min" : "Less than a minute";
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;
  if (style === "short") {
    if (!hours) return `${minutes} min`;
    return remainingMinutes ? `${hours} h ${remainingMinutes} min` : `${hours} h`;
  }
  const minuteLabel = `${remainingMinutes} ${remainingMinutes === 1 ? "minute" : "minutes"}`;
  return hours ? `${hours} ${hours === 1 ? "hour" : "hours"} ${minuteLabel}` : minuteLabel;
}
