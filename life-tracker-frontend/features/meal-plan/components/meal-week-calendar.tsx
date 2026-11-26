"use client";

import { ChevronLeft, ChevronRight, LocateFixed } from "lucide-react";
import PulseButton from "@/components/ui/pulse-button";
import PulseCard from "@/components/ui/pulse-card";
import PulseIconButton from "@/components/ui/pulse-icon-button";
import { mealSlotColors, type MealSlot } from "@/features/meal-plan/components/meal-visuals";
import type { MealWeekDay } from "@/features/meal-plan/hooks/use-meal-week";
import { mealPlanStyles as styles } from "@/features/meal-plan/styles/meal-plan-classes";

export interface PlannedDay {
  count: number;
  slots: Set<MealSlot>;
}

export default function MealWeekCalendar({
  weekDays, plannedByDay, calendarSlots, currentMonday, weekEnd, isToday, isLoadingWeek, onMoveWeek, onToday, onSelectDate,
}: {
  weekDays: MealWeekDay[];
  plannedByDay: PlannedDay[];
  calendarSlots: MealSlot[];
  currentMonday: Date;
  weekEnd: Date;
  isToday: boolean;
  isLoadingWeek: boolean;
  onMoveWeek: (days: number) => void;
  onToday: () => void;
  onSelectDate: (dateStr: string) => void;
}) {
  const weekRange = new Intl.DateTimeFormat("en-IN", {
    timeZone: "UTC",
    month: "short",
    day: "numeric",
    ...(currentMonday.getUTCFullYear() !== weekEnd.getUTCFullYear() && { year: "numeric" }),
  }).formatRange(currentMonday, weekEnd);

  return (
    <PulseCard as="section" className={styles.calendar} aria-label="Weekly calendar">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h2 aria-live="polite" className={styles.weekRange}>
          {weekRange}
          {currentMonday.getUTCFullYear() === weekEnd.getUTCFullYear() && (
            <span className={`${styles.muted} mt-1.5 block text-sm font-semibold tracking-normal`}>{weekEnd.getUTCFullYear()}</span>
          )}
        </h2>
        <div className="flex items-center gap-2">
          <PulseIconButton variant="secondary" size="xl" className={styles.weekArrow} type="button" onClick={() => onMoveWeek(-7)} aria-label="Previous week">
            <ChevronLeft className="size-5" aria-hidden="true" />
          </PulseIconButton>
          <PulseButton variant="primary" size="xl" labelStyle="strong" className={styles.primaryAction} type="button"
            onClick={onToday} disabled={isToday} icon={<LocateFixed className="size-5" aria-hidden="true" />}
          >
            Today
          </PulseButton>
          <PulseIconButton variant="secondary" size="xl" className={styles.weekArrow} type="button" onClick={() => onMoveWeek(7)} aria-label="Next week">
            <ChevronRight className="size-5" aria-hidden="true" />
          </PulseIconButton>
        </div>
      </div>

      <div className={styles.weekDays} role="group" aria-label="Days of the week" aria-busy={isLoadingWeek}>
        {weekDays.map((day, index) => {
          const { count, slots } = plannedByDay[index];
          const mealCountLabel = count ? `, ${count} ${count === 1 ? "meal" : "meals"} planned` : "";
          return (
            <button
              key={day.dateStr}
              className={styles.day}
              type="button"
              aria-label={`${day.dayFull}, ${day.monthDay}${mealCountLabel}`}
              aria-pressed={day.isSelected}
              aria-current={day.isToday ? "date" : undefined}
              onClick={() => onSelectDate(day.dateStr)}
            >
              <span className={styles.dayLabel}>{day.dayShort}</span>
              <span className={styles.dayNumber}>{day.date.getUTCDate()}</span>
              <span className={styles.dots} aria-hidden="true">
                {calendarSlots.map(slot => (
                  <span key={slot} className={`${styles.dot} ${mealSlotColors[slot]}`} data-planned={slots.has(slot)} />
                ))}
              </span>
            </button>
          );
        })}
      </div>
    </PulseCard>
  );
}
