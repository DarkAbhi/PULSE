"use client";

import { useState, useTransition, type ReactNode } from "react";
import Link from "next/link";
import { ChevronLeft, ChevronRight } from "lucide-react";
import PulseButton from "@/app/components/design-system/pulse-button";
import PulseIconButton from "@/app/components/design-system/pulse-icon-button";
import PulseConfirmationDialog from "@/app/components/design-system/pulse-confirmation-dialog";
import PulseCard from "@/app/components/design-system/pulse-card";
import { formatDuration } from "@/app/fitness/[id]/format-duration.mjs";
import { workoutLabel, type WorkoutScheduleDay } from "@/app/fitness/workout-types";
import type { FitnessOverview } from "@/app/fitness/types";
import { calendarDays, shiftMonth } from "@/app/fitness/calendar-utils.mjs";
import { markGymVisitAction } from "@/app/fitness/actions";
import { fitnessStyles as styles } from "@/app/fitness/fitness-classes";

const monthFormatter = new Intl.DateTimeFormat("en-IN", { month: "long", timeZone: "UTC" });
const dayFormatter = new Intl.DateTimeFormat("en-IN", { dateStyle: "full", timeZone: "UTC" });
const selectedDayFormatter = new Intl.DateTimeFormat("en-IN", {
  weekday: "short", day: "numeric", month: "short", timeZone: "UTC",
});
const weekdays = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

function parseDate(date: string) {
  return new Date(`${date}T00:00:00Z`);
}

export default function FitnessCalendar({ today, workoutsByDate, schedule, summary, children }: {
  today: string;
  workoutsByDate: FitnessOverview["calendar_workouts"];
  schedule: WorkoutScheduleDay[];
  summary: ReactNode;
  children: ReactNode;
}) {
  const [selectedDate, setSelectedDate] = useState(today);
  const [isMarking, startMarking] = useTransition();
  const [markError, setMarkError] = useState("");
  const [dateToMark, setDateToMark] = useState<string | null>(null);
  const month = selectedDate.slice(0, 7);
  const selectedVisits = workoutsByDate[selectedDate]?.workouts ?? [];
  const selectedWeekday = parseDate(selectedDate).getUTCDay() || 7;
  const plan = selectedDate >= today && schedule.find((day) => day.weekday === selectedWeekday);
  const selectedDayHeading = (
    <h3 className="text-lg leading-tight font-extrabold tracking-tight">
      <time dateTime={selectedDate}>{selectedDayFormatter.format(parseDate(selectedDate))}</time>
    </h3>
  );
  function navigateMonth(offset: number) {
    const next = shiftMonth(month, offset);
    setSelectedDate(next === today.slice(0, 7) ? today : `${next}-01`);
  }

  function openLog() {
    setMarkError("");
    setDateToMark(selectedDate);
  }

  return (
    <div className="grid gap-6 sm:gap-8">
      <div className="grid gap-4 lg:grid-cols-[minmax(0,1.2fr)_minmax(0,1fr)] lg:gap-6">
        <PulseCard as="section" aria-label="Workout calendar" padding="md" className="min-w-0">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <h2 aria-live="polite" className="text-2xl leading-none font-extrabold tracking-tight sm:text-3xl">
              {monthFormatter.format(parseDate(`${month}-01`))}
              <span className={`${styles.muted} mt-1 block text-sm font-semibold tracking-normal`}>{month.slice(0, 4)}</span>
            </h2>
            <div className="ml-auto flex items-center gap-2">
              {selectedDate <= today && (
                <PulseButton type="button" size="lg" variant="primary" className="whitespace-nowrap [--button-fill:var(--fitness-gradient)] [--button-foreground:white]" disabled={isMarking} onClick={openLog}>
                  Log Visit
                </PulseButton>
              )}
              <PulseIconButton type="button" variant="secondary" size="lg" aria-label="Previous month" onClick={() => navigateMonth(-1)}>
                <ChevronLeft size={20} aria-hidden="true" />
              </PulseIconButton>
              <PulseIconButton type="button" variant="secondary" size="lg" aria-label="Next month" onClick={() => navigateMonth(1)}>
                <ChevronRight size={20} aria-hidden="true" />
              </PulseIconButton>
            </div>
          </div>
          <div role="group" aria-label="Select a date" className="grid grid-cols-7 gap-1 sm:gap-2">
            {weekdays.map((weekday) => (
              <span key={weekday} aria-hidden="true" className={`${styles.muted} pb-1 text-center text-xs font-bold sm:text-sm`}>{weekday}</span>
            ))}
            {calendarDays(month).map((date) => {
              const count = workoutsByDate[date]?.workouts.length ?? 0;
              return (
                <button
                  key={date}
                  type="button"
                  aria-pressed={date === selectedDate}
                  aria-current={date === today ? "date" : undefined}
                  aria-label={`${dayFormatter.format(parseDate(date))}, ${count ? `${count} completed workout${count === 1 ? "" : "s"}` : "no workouts"}`}
                  data-outside={date.slice(0, 7) !== month}
                  data-workout={count > 0}
                  onClick={() => setSelectedDate(date)}
                  className={styles.day}
                >
                  {Number(date.slice(8))}
                </button>
              );
            })}
          </div>
          <ul aria-hidden="true" className={`${styles.muted} mt-4 flex flex-wrap gap-x-4 gap-y-2 text-xs font-semibold sm:text-sm`}>
            <li className="flex items-center gap-1.5"><span className={styles.legendToday} />Today</li>
            <li className="flex items-center gap-1.5"><span className={styles.legendSelected} />Selected</li>
            <li className="flex items-center gap-1.5"><span className={styles.legendWorkout} />Workout logged</li>
          </ul>
          <div aria-live="polite" className={styles.selectedDate}>
            {selectedVisits.length > 0 ? (
              <ul role="list" className="divide-y divide-divider">
                {selectedVisits.map((visit, index) => {
                  const label = workoutLabel(visit);
                  const duration = visit.duration_seconds == null ? "" : ` for ${formatDuration(visit.duration_seconds, "short")}`;
                  return (
                    <li key={visit.id} className="flex flex-wrap items-center justify-between gap-x-4 gap-y-2 py-3">
                      <div className="min-w-32 flex-1">
                        {index === 0 && selectedDayHeading}
                        <p className={`${styles.selectedDateDetails} tabular-nums wrap-anywhere`}>{label ?? "Gym visit"} at {visit.time_label}{duration}</p>
                      </div>
                      <Link
                        href={`/fitness/${visit.id}`}
                        aria-label={`View workout for ${label ?? "gym visit"} at ${visit.time_label}${duration}`}
                        className="inline-flex min-h-11 shrink-0 items-center gap-2 rounded-full bg-chip px-4 text-sm font-bold hover:bg-divider"
                      >
                        View workout
                        <ChevronRight aria-hidden="true" className="size-4" />
                      </Link>
                    </li>
                  );
                })}
              </ul>
            ) : (
              <div className="py-3">
                {selectedDayHeading}
                <p className={styles.selectedDateDetails}>No workout logged</p>
              </div>
            )}
            {plan && <p className="border-t border-divider py-2 text-control font-semibold wrap-anywhere"><span className={styles.muted}>Scheduled</span> · {workoutLabel(plan)}</p>}
          </div>
        </PulseCard>
        {summary}
      </div>

      {children}

      <PulseConfirmationDialog
        isOpen={dateToMark !== null}
        onClose={() => { setDateToMark(null); setMarkError(""); }}
        onConfirm={() => {
          if (!dateToMark) return;
          setMarkError("");
          startMarking(async () => {
            const result = await markGymVisitAction(dateToMark);
            if (!result.ok) {
              setMarkError(result.error ?? "We couldn't save your gym visit.");
              return;
            }
            setSelectedDate(dateToMark);
            setDateToMark(null);
          });
        }}
        title="Log gym visit?"
        description={dateToMark ? `Log a gym visit for ${dayFormatter.format(parseDate(dateToMark))}?` : ""}
        confirmText="Log gym visit"
        confirmLoadingText="Logging…"
        isLoading={isMarking}
        error={markError}
        variant="positive"
      />
    </div>
  );
}
