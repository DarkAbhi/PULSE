"use client";

import PulseCard from "@/app/components/design-system/pulse-card";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { ChevronLeft, ChevronRight } from "lucide-react";
import Button from "@/app/components/design-system/button";
import ConfirmationDialog from "@/app/components/design-system/confirmation-dialog";
import GymVisitsList, { type WorkoutGroup } from "./gym-visits-list";
import { calendarDays, shiftMonth } from "./calendar-utils.mjs";
import { markGymVisitAction } from "./actions";

const monthFormatter = new Intl.DateTimeFormat("en-IN", {
  month: "long", year: "numeric", timeZone: "UTC",
});
const dayFormatter = new Intl.DateTimeFormat("en-IN", {
  dateStyle: "full", timeZone: "UTC",
});

const shortDayFormatter = new Intl.DateTimeFormat("en-IN", {
  day: "numeric", month: "short", timeZone: "UTC",
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
  const day = new Date(`${selectedDate}T00:00:00Z`);
  const summaryCards = cards.filter(({ id }) => id === "last_workout" || id === "streak");
  const yearCards = cards.filter(({ id }) => id !== "last_workout" && id !== "streak");

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
    <div className="space-y-8">
      <PulseCard className="grid items-start lg:grid-cols-[1.3fr_1fr] lg:gap-6 lg:bg-transparent">
        <PulseCard as="section" aria-label="Workout calendar" className="p-4 sm:p-6">
          <div className="flex items-center justify-between gap-3 border-b border-divider pb-2 sm:pb-3">
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
          <div className="mt-3 grid grid-cols-7 text-center text-xs font-semibold text-muted-foreground sm:text-sm" aria-hidden="true">
            {["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"].map((day) => <span key={day}>{day}</span>)}
          </div>
          <div className="mt-2 grid grid-cols-7 gap-y-1 sm:gap-y-2">
            {calendarDays(month).map((date) => {
              const count = workoutsByDate[date]?.workouts.length ?? 0;
              const selected = date === selectedDate;
              return (
                <div key={date} className="flex min-w-0 justify-center">
                  <button
                    type="button"
                    aria-pressed={selected}
                    aria-current={date === today ? "date" : undefined}
                    aria-label={`${dayFormatter.format(new Date(`${date}T00:00:00Z`))}, ${count ? `${count} completed workout${count === 1 ? "" : "s"}` : "no workouts"}`}
                    onClick={() => selectDate(date)}
                    className={`relative flex h-11 w-full max-w-11 items-center justify-center rounded-full text-sm font-semibold transition sm:text-base ${selected ? "bg-primary text-primary-foreground" : count ? "border-2 border-success bg-success-soft text-success hover:bg-success-soft dark:bg-success/15 dark:text-success dark:hover:bg-success/25" : date.slice(0, 7) !== month ? "text-muted-foreground/50 hover:bg-secondary" : "text-foreground hover:bg-secondary"}`}
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
          <p className="mt-3 flex items-center gap-2 text-xs text-muted-foreground">
            <span aria-hidden="true" className="h-2 w-2 rounded-full bg-success" />
            Workout completed
          </p>
        </PulseCard>
        <PulseCard
          as="section"
          aria-labelledby="selected-workouts-heading"
          className="border-t border-divider p-4 max-lg:rounded-t-none sm:p-6 lg:border-t-0"
        >
          <div aria-live="polite" className="space-y-3">
            <h2 id="selected-workouts-heading" className="text-lg font-semibold sm:text-xl">
              {selectedDate === today ? `Today, ${shortDayFormatter.format(day)}` : dayFormatter.format(day)}
            </h2>
            {selectedGroup ? <GymVisitsList groups={[selectedGroup]} hideDate /> : (
              <div className="flex flex-wrap items-center justify-between gap-x-3 gap-y-2">
                <p className="text-sm text-muted-foreground">No workout logged</p>
                <Button
                  type="button"
                  size="lg"
                  disabled={isMarking}
                  onClick={() => {
                    setMarkError("");
                    setDateToMark(selectedDate);
                  }}
                >
                  {isMarking ? "Logging…" : "Log gym visit"}
                </Button>
              </div>
            )}
          </div>
        </PulseCard>
      </PulseCard>
      <div className="grid items-start gap-8 lg:grid-cols-[1.3fr_1fr] lg:gap-6">
        <section aria-labelledby="recent-workouts-heading" className="space-y-3 lg:col-start-2 lg:row-start-1">
          <h2 id="recent-workouts-heading" className="text-xl font-semibold">Recent workouts</h2>
          {recentWorkouts.length ? <GymVisitsList groups={recentWorkouts} /> : (
            <PulseCard className="p-4">
              <p className="font-semibold">No workouts yet</p>
              <p className="mt-1 text-sm text-muted-foreground">Select a date in the calendar to log a gym visit.</p>
            </PulseCard>
          )}
        </section>
        <section aria-labelledby="fitness-stats-heading" className="space-y-3 lg:col-start-1 lg:row-start-1">
          <h2 id="fitness-stats-heading" className="text-xl font-semibold">Your fitness stats</h2>
          <PulseCard className="grid grid-cols-2 divide-x divide-divider">
            {summaryCards.map((card) => (
              <div key={card.id} className="min-w-0 p-3 sm:p-4">
                <h3 className="text-sm font-medium">{card.label}</h3>
                <p className="mt-1 break-words text-xl font-bold tracking-tight sm:text-2xl">{card.value}</p>
                <p className="text-xs text-muted-foreground">{card.subtitle}</p>
              </div>
            ))}
          </PulseCard>
          <PulseCard className="px-4 pt-4">
            <h3 className="text-sm font-semibold">Year to date</h3>
            <dl className="mt-1 divide-y divide-divider">
              {yearCards.map((card) => {
                const note = card.subtitle.replace(/(?:^This year$| this year\b)/i, "");
                return (
                  <div key={card.id} className="flex items-baseline justify-between gap-3 py-2.5 text-sm">
                    <dt>
                      {card.label}
                      {(note || card.tooltip) && (
                        <span className="block text-xs text-muted-foreground">
                          {[note, card.tooltip].filter(Boolean).join(". ")}
                        </span>
                      )}
                    </dt>
                    <dd className="max-w-[50%] break-words text-right font-semibold tabular-nums">{card.value}</dd>
                  </div>
                );
              })}
            </dl>
          </PulseCard>
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
              setMarkError(result.error ?? "We couldn't save your gym visit.");
              return;
            }
            setDateToMark(null);
            router.refresh();
          });
        }}
        title="Log gym visit?"
        description={dateToMark ? `Log a gym visit for ${dayFormatter.format(new Date(`${dateToMark}T00:00:00Z`))}?` : ""}
        confirmText="Log gym visit"
        confirmLoadingText="Logging…"
        isLoading={isMarking}
        error={markError}
        variant="positive"
      />
    </div>
  );
}
