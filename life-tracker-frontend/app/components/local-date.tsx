"use client";
import { useTimeZone } from "./time-zone-provider";
import { formatInstant, formatCalendarDate } from "@/app/lib/time-zone.mjs";

interface LocalDateProps { dateString: string; options?: Intl.DateTimeFormatOptions; calendarOnly?: boolean; }
export default function LocalDate({ dateString, options, calendarOnly = false }: LocalDateProps) {
  const { timeZone } = useTimeZone();
  const formatted = calendarOnly ? formatCalendarDate(dateString, options ?? { dateStyle: "medium" }) : formatInstant(dateString, timeZone, options);
  return <time dateTime={dateString}>{formatted}</time>;
}
