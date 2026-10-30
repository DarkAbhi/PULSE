"use client";

import PulseCard from "../components/design-system/pulse-card";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { ChevronLeft, ChevronRight, Info } from "lucide-react";
import Button from "../components/design-system/button";
import ConfirmationDialog from "../components/design-system/confirmation-dialog";
import GymVisitsList, { type WorkoutGroup } from "./gym-visits-list";
import { calendarDays, shiftMonth } from "./calendar-utils.mjs";
import { markGymVisitAction } from "./actions";

const monthFormatter = new Intl.DateTimeFormat("en-IN", {
  month: "long", year: "numeric", timeZone: "UTC",
});
const dayFormatter = new Intl.DateTimeFormat("en-IN", {
  dateStyle: "full", timeZone: "UTC",
});

export type FitnessOverview = {
  today: string;
  cards: { id: string; label: string; value: string; subtitle: string; tooltip?: string }[];
  calendar_workouts: Record<string, WorkoutGroup>;
  recent_workouts: WorkoutGroup[];
};

export default function FitnessCalendar({ overview }: { overview: FitnessOverview }) {
  const { today, cards, calendar_workouts: workoutsByDate, recent_workouts: recentWorkouts } = overview;
  const [month, setMonth] = useState(today.slice(0, 7));
  const [selectedDate, setSelectedDate] = useState(today);
  const [isMarking, startMarking] = useTransition();
  const [markError, setMarkError] = useState("");
  const [dateToMark, setDateToMark] = useState<string | null>(null);
  const router = useRouter();
  const selectedGroup = workoutsByDate[selectedDate];

  function selectDate(date: string) {
    setMarkError("");
    setSelectedDate(date);
    setMonth(date.slice(0, 7));
  }

  function navigateMonth(offset: number) {
    const next = shiftMonth(month, offset);
    selectDate(next === today.slice(0, 7) ? today : `${next}-01`);
  }

  return (
    <div className="space-y-10">
      <div className="grid items-start gap-6 lg:grid-cols-[1.3fr_1fr]">
        <PulseCard as="section" aria-label="Workout calendar" className="p-4 sm:p-6">
          <div className="flex items-center justify-between gap-3 border-b border-border pb-5">
            <h2 aria-live="polite" className="text-xl font-semibold sm:text-2xl">
              {monthFormatter.format(new Date(`${month}-01T00:00:00Z`))}
            </h2>
            <div className="flex gap-1">
              <Button type="button" variant="icon" aria-label="Previous month" onClick={() => navigateMonth(-1)}>
                <ChevronLeft className="h-5 w-5" aria-hidden="true" />
              </Button>
              <Button type="button" variant="icon" aria-label="Next month" onClick={() => navigateMonth(1)}>
                <ChevronRight className="h-5 w-5" aria-hidden="true" />
              </Button>
            </div>
          </div>
          <div className="mt-5 grid grid-cols-7 text-center text-xs font-semibold text-muted-foreground sm:text-sm" aria-hidden="true">
            {["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"].map((day) => <span key={day}>{day}</span>)}
          </div>
          <div className="mt-3 grid grid-cols-7 gap-y-3 sm:gap-y-5">
            {calendarDays(month).map((date) => {
              const count = workoutsByDate[date]?.workouts.length ?? 0;
              const selected = date === selectedDate;
              return (
                <div key={date} className="flex justify-center py-1">
                  <button
                    type="button"
                    aria-pressed={selected}
                    aria-current={date === today ? "date" : undefined}
                    aria-label={`${dayFormatter.format(new Date(`${date}T00:00:00Z`))}, ${count ? `${count} completed workout${count === 1 ? "" : "s"}` : "no workouts"}`}
                    onClick={() => selectDate(date)}
                    className={`relative flex h-10 w-10 items-center justify-center rounded-full border-2 text-sm font-semibold transition focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-ring/30 sm:h-11 sm:w-11 sm:text-base ${selected ? "border-transparent bg-primary text-primary-foreground" : count ? "border-success bg-success-soft text-success hover:bg-success-soft dark:border-success dark:bg-success/15 dark:text-success dark:hover:bg-success/25" : date.slice(0, 7) !== month ? "border-transparent text-muted-foreground/50 hover:bg-secondary" : "border-transparent text-foreground hover:bg-secondary"}`}
                  >
                    {Number(date.slice(8))}
                    {count > 0 && (
                      <span aria-hidden="true" className="absolute -bottom-1 h-2 w-2 rounded-full bg-success ring-2 ring-card dark:bg-success" />
                    )}
                  </button>
                </div>
              );
            })}
          </div>
          <p className="mt-5 flex items-center gap-2 text-xs text-muted-foreground">
            <span aria-hidden="true" className="h-2 w-2 rounded-full bg-success" />
            Workout completed
          </p>
        </PulseCard>
        <section aria-label="Selected day's workouts" className="space-y-4">
          <h2 className="text-xl font-semibold">Workouts for this day</h2>
          <div aria-live="polite">
            {selectedGroup ? <GymVisitsList groups={[selectedGroup]} /> : (
              <PulseCard className="p-6">
                <p className="text-sm font-semibold">{dayFormatter.format(new Date(`${selectedDate}T00:00:00Z`))}</p>
                <p className="mt-2 text-sm text-muted-foreground">No workouts recorded for this day.</p>
                <Button
                  type="button"
                  size="sm"
                  className="mt-4"
                  disabled={isMarking}
                  onClick={() => {
                    setMarkError("");
                    setDateToMark(selectedDate);
                  }}
                >
                  {isMarking ? "Marking…" : "Mark workout"}
                </Button>
              </PulseCard>
            )}
          </div>
        </section>
      </div>
      <div className="grid items-start gap-6 lg:grid-cols-[1.3fr_1fr]">
        <section aria-labelledby="fitness-stats-heading" className="space-y-4">
          <h2 id="fitness-stats-heading" className="text-xl font-semibold">Your fitness stats</h2>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            {cards.map((card) => (
              <PulseCard key={card.id} className="flex min-h-44 flex-col items-center justify-center px-4 py-6 text-center">
                <h3 className="text-base font-medium">{card.label}</h3>
                <p className="my-3 max-w-full break-words text-3xl font-bold tracking-tight sm:text-4xl">{card.value}</p>
                <div className="flex items-center gap-1.5 text-sm text-muted-foreground">
                  <span>{card.subtitle}</span>
                  {card.tooltip && (
                    <span className="group relative inline-flex">
                      <button type="button" aria-label={`${card.label} information`} aria-describedby={`stat-tooltip-${card.id}`} className="rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
                        <Info className="h-4 w-4" aria-hidden="true" />
                      </button>
                      <span id={`stat-tooltip-${card.id}`} role="tooltip" className="pointer-events-none invisible absolute bottom-full left-1/2 z-10 mb-2 w-52 -translate-x-1/2 rounded-lg border border-border bg-popover px-3 py-2 text-center text-xs text-popover-foreground shadow-md group-hover:visible group-focus-within:visible">
                        {card.tooltip}
                      </span>
                    </span>
                  )}
                </div>
              </PulseCard>
            ))}
          </div>
        </section>
        <section aria-labelledby="recent-workouts-heading" className="space-y-4">
          <h2 id="recent-workouts-heading" className="text-xl font-semibold">Recent workouts</h2>
          {recentWorkouts.length ? <GymVisitsList groups={recentWorkouts} /> : (
            <PulseCard className="p-6">
              <p className="font-semibold">No workouts yet.</p>
              <p className="mt-2 text-sm text-muted-foreground">Mark a workout completed when you&apos;re ready.</p>
            </PulseCard>
          )}
        </section>
      </div>
      <ConfirmationDialog
        isOpen={dateToMark !== null}
        onClose={() => { setDateToMark(null); setMarkError(""); }}
        onConfirm={() => {
          if (!dateToMark) return;
          setMarkError("");
          startMarking(async () => {
            const result = await markGymVisitAction(dateToMark);
            if (!result.ok) {
              setMarkError(result.error ?? "We couldn't save your workout.");
              return;
            }
            setDateToMark(null);
            router.refresh();
          });
        }}
        title="Mark workout?"
        description={dateToMark ? `Mark a workout as completed for ${dayFormatter.format(new Date(`${dateToMark}T00:00:00Z`))}?` : ""}
        confirmText="Mark workout"
        confirmLoadingText="Marking…"
        isLoading={isMarking}
        error={markError}
        variant="positive"
      />
    </div>
  );
}
