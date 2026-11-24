"use client";

import { useId, useMemo, useState } from "react";
import type { KeyboardEvent } from "react";
import type { QuestActivity } from "@/app/quests/types";

const daysInGraph = 365;
const weekdayLabels = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const questColors = new Map([
  ["gym_workout", "var(--dashboard-quest-gym)"],
  ["daily_steps", "var(--dashboard-quest-steps)"],
  ["activity_rings", "var(--dashboard-quest-rings)"],
  ["sleep_goal", "var(--dashboard-quest-sleep)"],
  ["daily_meals", "var(--dashboard-quest-meals)"],
]);
const monthFormat = new Intl.DateTimeFormat("en", { month: "short", timeZone: "UTC" });
const dateFormat = new Intl.DateTimeFormat("en", { dateStyle: "full", timeZone: "UTC" });
const emptyColor = "var(--dashboard-quest-empty)";
const palette = Array.from(questColors.values());
const dayOffsets = new Map([["ArrowLeft", -7], ["ArrowRight", 7], ["ArrowUp", -1], ["ArrowDown", 1]]);

function blendColors(colors: string[]) {
  // Give each completed quest an equal share of the blend.
  return colors.reduce((mix, color, index) =>
    `color-mix(in oklab, ${mix}, ${color} ${100 / (index + 1)}%)`);
}

function cellColor(count: number, hue: string) {
  if (count === 0) return emptyColor;
  const strength = 44 + 56 * Math.min(count, palette.length) / palette.length;
  return `color-mix(in oklab, ${emptyColor}, ${hue} ${strength}%)`;
}

function buildCalendar({ today, days }: QuestActivity) {
  const completions = new Map(days.map(({ date, completed_ids }) => [date, completed_ids]));
  // These are calendar dates in the user's Pulse timezone. UTC arithmetic keeps
  // consecutive dates evenly spaced across daylight saving changes.
  const first = new Date(`${today}T00:00:00Z`);
  first.setUTCDate(first.getUTCDate() - (daysInGraph - 1));
  const offset = first.getUTCDay();
  const cursor = new Date(first);
  return Array.from({ length: daysInGraph }, (_, index) => {
    const date = new Date(cursor);
    cursor.setUTCDate(cursor.getUTCDate() + 1);
    const key = date.toISOString().slice(0, 10);
    const colors = (completions.get(key) ?? []).map((id) => questColors.get(id) ?? "var(--dashboard-quest-sleep)");
    const count = colors.length;
    const summary = `${count} ${count === 1 ? "quest" : "quests"} completed`;
    return {
      date,
      key,
      week: Math.floor((index + offset) / 7),
      weekday: date.getUTCDay(),
      summary,
      color: count > 0 ? cellColor(count, blendColors(colors)) : emptyColor,
    };
  });
}

const legendHue = blendColors(palette);
const cellClass = "aspect-square rounded-sm border border-foreground/10";

export default function QuestHeatmap({ activity }: { activity: QuestActivity }) {
  const [selectedDate, setSelectedDate] = useState(activity.today);
  const [hoveredDate, setHoveredDate] = useState<string | null>(null);
  const instructionsId = useId();
  const calendar = useMemo(() => buildCalendar(activity), [activity]);
  const today = calendar[calendar.length - 1];
  const selected = calendar.find(({ key }) => key === selectedDate) ?? today;
  const active = calendar.find(({ key }) => key === hoveredDate) ?? selected;
  const weekCount = today.week + 1;
  const months = calendar.filter(({ date }) => date.getUTCDate() === 1);
  if (months[0].week >= 3) months.unshift(calendar[0]);
  const years = calendar.filter(({ date }, index) =>
    index === 0 || (date.getUTCMonth() === 0 && date.getUTCDate() === 1));
  const combinedYears = years.length > 1 && years[1].week < 3;

  function moveFocus(event: KeyboardEvent<HTMLDivElement>) {
    const offset = dayOffsets.get(event.key);
    if (offset == null) return;
    event.preventDefault();
    const index = calendar.indexOf(selected);
    const next = index + offset;
    if (next < 0 || next >= calendar.length) return;
    if (Math.abs(offset) === 1 && calendar[next].week !== selected.week) return;
    event.currentTarget.querySelector<HTMLButtonElement>(`button[data-date="${calendar[next].key}"]`)?.focus();
  }

  return (
    <>
      <p id={instructionsId} className="sr-only">Use the arrow keys to explore dates.</p>
      <div dir="rtl" className="overflow-x-auto">
        <div
          dir="ltr"
          role="group"
          aria-label="Daily quest completions"
          aria-describedby={instructionsId}
          onKeyDown={moveFocus}
          onPointerLeave={() => setHoveredDate(null)}
          className="grid items-center gap-1 p-1"
          style={{
            gridTemplateColumns: `2rem repeat(${weekCount}, minmax(0.75rem, 1fr))`,
            minWidth: `calc(2.5rem + ${weekCount} * 1rem)`,
          }}
        >
          {combinedYears ? (
            <span aria-hidden="true" className="col-start-2 col-span-5 row-start-1 whitespace-nowrap text-xs font-semibold">
              {years[0].date.getUTCFullYear()}–{years[1].date.getUTCFullYear()}
            </span>
          ) : years.map(({ key, date, week }) => (
            <span key={key} aria-hidden="true"
              style={{ gridRow: 1, gridColumn: `${Math.min(week, weekCount - 3) + 2} / span 3` }}
              className={`text-xs font-semibold ${week > weekCount - 3 ? "text-right" : ""}`}>
              {date.getUTCFullYear()}
            </span>
          ))}
          {months.map(({ key, date, week }) => (
            <span key={key} aria-hidden="true"
              style={{ gridRow: 2, gridColumn: `${Math.min(week, weekCount - 3) + 2} / span 3` }}
              className={`text-xs text-muted-foreground ${week > weekCount - 3 ? "text-right" : ""}`}>
              {monthFormat.format(date)}
            </span>
          ))}
          {weekdayLabels.map((label, weekday) => (
            <span key={label} aria-hidden="true" style={{ gridRow: weekday + 3, gridColumn: 1 }}
              className="sticky left-0 z-10 bg-card text-[11px] leading-none text-muted-foreground">
              {label}
            </span>
          ))}
          {calendar.map(({ date, key, week, weekday, color, summary }) => (
            <button
              key={key}
              type="button"
              data-date={key}
              aria-label={`${dateFormat.format(date)}, ${summary}`}
              aria-current={key === activity.today ? "date" : undefined}
              tabIndex={key === selected.key ? 0 : -1}
              onFocus={() => {
                setSelectedDate(key);
                setHoveredDate(null);
              }}
              onClick={() => setSelectedDate(key)}
              onPointerEnter={(event) => {
                if (event.pointerType === "mouse") setHoveredDate(key);
              }}
              className={`${cellClass} w-full hover:border-foreground focus-visible:z-10 ${key === active.key ? "ring-1 ring-foreground/65" : ""}`}
              style={{ gridRow: weekday + 3, gridColumn: week + 2, backgroundColor: color }}
            />
          ))}
        </div>
      </div>
      <div role="group" className="flex items-center justify-end gap-1.5 whitespace-nowrap text-xs text-muted-foreground"
        aria-label="Less to more quests completed per day">
        <span>Less</span>
        {Array.from({ length: palette.length + 1 }, (_, count) => (
          <span key={count} title={`${count} ${count === 1 ? "quest" : "quests"} completed`}
            className={`${cellClass} size-3`} style={{ backgroundColor: cellColor(count, legendHue) }} />
        ))}
        <span>More</span>
      </div>
      <p className="text-sm text-muted-foreground">
        <strong className="font-semibold text-foreground">{dateFormat.format(active.date)}</strong>
        {" · "}{active.summary}
      </p>
    </>
  );
}
