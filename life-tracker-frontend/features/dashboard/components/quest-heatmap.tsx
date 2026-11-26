"use client";

import { useMemo, useState } from "react";
import type { KeyboardEvent } from "react";
import type { QuestActivity } from "@/features/quests";

const daysInGraph = 365;
const weekdayLabels = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const monthFormat = new Intl.DateTimeFormat("en-US", { month: "short", timeZone: "UTC" });
const dateFormat = new Intl.DateTimeFormat("en-US", { dateStyle: "full", timeZone: "UTC" });
const dayOffsets = new Map([["ArrowLeft", -7], ["ArrowRight", 7], ["ArrowUp", -1], ["ArrowDown", 1]]);
const levels = [
  { color: "bg-(--dashboard-activity-chip)", label: "No quests completed" },
  { color: "bg-(--dashboard-quest-level-1)", label: "1 quest completed" },
  { color: "bg-(--dashboard-quest-level-2)", label: "2 quests completed" },
  { color: "bg-(--dashboard-quest-level-3)", label: "3–4 quests completed" },
  { color: "bg-(--dashboard-quest-level-4)", label: "5 or more quests completed" },
];

function questLevel(count: number) {
  if (count <= 2) return levels[count];
  return count <= 4 ? levels[3] : levels[4];
}

function buildCalendar({ today, days }: QuestActivity) {
  const completions = new Map(days.map(({ date, completed_ids }) => [date, completed_ids.length]));
  // UTC arithmetic keeps the user's calendar dates evenly spaced across DST changes.
  const first = new Date(`${today}T00:00:00Z`);
  first.setUTCDate(first.getUTCDate() - (daysInGraph - 1));
  const offset = first.getUTCDay();
  const cursor = new Date(first);

  return Array.from({ length: daysInGraph }, (_, index) => {
    const date = new Date(cursor);
    cursor.setUTCDate(cursor.getUTCDate() + 1);
    const key = date.toISOString().slice(0, 10);
    const count = completions.get(key) ?? 0;
    return {
      date,
      key,
      week: Math.floor((index + offset) / 7),
      weekday: date.getUTCDay(),
      summary: count === 0 ? "No quests completed" : `${count} ${count === 1 ? "quest" : "quests"} completed`,
      color: questLevel(count).color,
    };
  });
}

export default function QuestHeatmap({ activity }: { activity: QuestActivity }) {
  const [selectedDate, setSelectedDate] = useState(activity.today);
  const calendar = useMemo(() => buildCalendar(activity), [activity]);
  const today = calendar[calendar.length - 1];
  const selected = calendar.find(({ key }) => key === selectedDate) ?? today;
  const weekCount = today.week + 1;
  const months = calendar.filter(({ date, weekday }) => weekday === 0 && date.getUTCDate() <= 7);

  function moveFocus(event: KeyboardEvent<HTMLDivElement>) {
    const offset = dayOffsets.get(event.key);
    if (offset == null) return;
    event.preventDefault();
    const next = calendar[calendar.indexOf(selected) + offset];
    if (next) event.currentTarget.querySelector<HTMLButtonElement>(`button[data-date="${next.key}"]`)?.focus();
  }

  return (
    <>
      <div dir="rtl" className="-mx-1.5 -mt-1.5 overflow-x-auto [scrollbar-width:thin]">
        <div
          dir="ltr"
          role="group"
          aria-label="Quests completed each day over the past year. Use the arrow keys to move between days."
          onKeyDown={moveFocus}
          className="grid gap-1 px-1.5 pt-1.5 pb-2.5"
          style={{
            gridTemplateColumns: `34px repeat(${weekCount}, minmax(13px, 1fr))`,
            gridTemplateRows: "auto repeat(7, auto)",
            minWidth: `calc(48px + ${weekCount} * 17px)`,
          }}
        >
          {months.map(({ key, date, week }) => (
            <span key={key} aria-hidden="true"
              style={{ gridRow: 1, gridColumn: `${Math.min(week, weekCount - 3) + 2} / span 3` }}
              className={`self-end pb-1 text-[.78rem] font-semibold whitespace-nowrap text-muted-foreground ${week > weekCount - 3 ? "text-right" : ""}`}>
              {monthFormat.format(date)}
            </span>
          ))}
          {weekdayLabels.map((label, weekday) => (
            <span key={label} aria-hidden="true" style={{ gridRow: weekday + 2, gridColumn: 1 }}
              className="self-center text-[.74rem] font-semibold text-muted-foreground">
              {label}
            </span>
          ))}
          {calendar.map(({ date, key, week, weekday, color, summary }) => {
            const isSelected = key === selected.key;
            const isToday = key === today.key;
            const label = `${dateFormat.format(date)} · ${summary}`;
            const outline = isSelected
              ? "relative z-1 shadow-[0_0_0_2px_var(--card),0_0_0_4px_var(--foreground)]"
              : isToday ? "shadow-[0_0_0_2px_var(--card),0_0_0_4px_var(--brand-pink)]" : "";
            return (
              <button
                key={key}
                type="button"
                data-date={key}
                aria-label={label}
                title={label}
                aria-current={isToday ? "date" : undefined}
                aria-pressed={isSelected}
                tabIndex={isSelected ? 0 : -1}
                onFocus={() => setSelectedDate(key)}
                onClick={() => setSelectedDate(key)}
                className={`aspect-square w-full cursor-pointer rounded-[5px] hover:z-2 focus-visible:z-2 motion-safe:transition-transform motion-safe:hover:scale-[1.18] ${color} ${outline}`}
                style={{ gridRow: weekday + 2, gridColumn: week + 2 }}
              />
            );
          })}
        </div>
      </div>
      <div className="mt-3.5 flex flex-wrap items-center justify-between gap-4">
        <p className="text-muted-foreground" aria-live="polite">
          <strong className="font-extrabold text-foreground">{dateFormat.format(selected.date)}</strong>
          {" · "}{selected.summary}
        </p>
        <div role="group" className="flex items-center gap-1.5 text-[.82rem] font-semibold whitespace-nowrap text-muted-foreground"
          aria-label="Less to more quests completed per day">
          <span>Less</span>
          {levels.map(({ label, color }) => (
            <span key={label} title={label} className={`size-3.5 rounded-sm ${color}`} />
          ))}
          <span>More</span>
        </div>
      </div>
    </>
  );
}
