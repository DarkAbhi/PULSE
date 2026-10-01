"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { ChevronLeft, ChevronRight } from "lucide-react";
import Button from "../components/design-system/button";
import GymVisitsList, { type GymVisit } from "./gym-visits-list";
import { calendarDays, indiaDateKey, shiftMonth } from "./calendar-utils.mjs";
import { markGymVisitAction } from "../dashboard/actions";

const monthFormatter = new Intl.DateTimeFormat("en-IN", {
  month: "long", year: "numeric", timeZone: "UTC",
});
const dayFormatter = new Intl.DateTimeFormat("en-IN", {
  dateStyle: "full", timeZone: "UTC",
});

export default function FitnessCalendar({ visits, today }: { visits: GymVisit[]; today: string }) {
  const [month, setMonth] = useState(today.slice(0, 7));
  const [selectedDate, setSelectedDate] = useState(today);
  const [isMarking, startMarking] = useTransition();
  const [markError, setMarkError] = useState("");
  const router = useRouter();
  const visitsByDate = new Map<string, GymVisit[]>();
  for (const visit of visits) {
    const key = indiaDateKey(new Date(visit.created_at));
    const dayVisits = visitsByDate.get(key) ?? [];
    dayVisits.push(visit);
    visitsByDate.set(key, dayVisits);
  }
  const selectedVisits = visitsByDate.get(selectedDate) ?? [];

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
        <section aria-label="Workout calendar" className="rounded-2xl border border-border bg-card p-4 shadow-sm sm:p-6">
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
              const count = visitsByDate.get(date)?.length ?? 0;
              const selected = date === selectedDate;
              return (
                <div key={date} className="flex justify-center py-1">
                  <button
                    type="button"
                    aria-pressed={selected}
                    aria-current={date === today ? "date" : undefined}
                    aria-label={`${dayFormatter.format(new Date(`${date}T00:00:00Z`))}, ${count ? `${count} completed workout${count === 1 ? "" : "s"}` : "no workouts"}`}
                    onClick={() => selectDate(date)}
                    className={`relative flex h-10 w-10 items-center justify-center rounded-full border-2 text-sm font-semibold transition focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-ring/30 sm:h-11 sm:w-11 sm:text-base ${selected ? "border-transparent bg-primary text-primary-foreground" : count ? "border-emerald-600 bg-emerald-50 text-emerald-900 hover:bg-emerald-100 dark:border-emerald-400 dark:bg-emerald-400/15 dark:text-emerald-200 dark:hover:bg-emerald-400/25" : date.slice(0, 7) !== month ? "border-transparent text-muted-foreground/50 hover:bg-secondary" : "border-transparent text-foreground hover:bg-secondary"}`}
                  >
                    {Number(date.slice(8))}
                    {count > 0 && (
                      <span aria-hidden="true" className="absolute -bottom-1 h-2 w-2 rounded-full bg-emerald-600 ring-2 ring-card dark:bg-emerald-400" />
                    )}
                  </button>
                </div>
              );
            })}
          </div>
          <p className="mt-5 flex items-center gap-2 text-xs text-muted-foreground">
            <span aria-hidden="true" className="h-2 w-2 rounded-full bg-emerald-600 dark:bg-emerald-400" />
            Workout completed · Dates in India time
          </p>
        </section>
        <section aria-label="Selected day's workouts" className="space-y-4">
          <h2 className="text-xl font-semibold">Workouts for this day</h2>
          <div aria-live="polite">
            {selectedVisits.length ? <GymVisitsList visits={selectedVisits} /> : (
              <div className="rounded-2xl border border-dashed border-border bg-card p-6">
                <p className="text-sm font-semibold">{dayFormatter.format(new Date(`${selectedDate}T00:00:00Z`))}</p>
                <p className="mt-2 text-sm text-muted-foreground">No workouts recorded for this day.</p>
                <Button
                  type="button"
                  size="sm"
                  className="mt-4"
                  disabled={isMarking}
                  onClick={() => {
                    setMarkError("");
                    startMarking(async () => {
                      const result = await markGymVisitAction(selectedDate);
                      if (!result.ok) {
                        setMarkError(result.error ?? "We couldn't save your workout.");
                        return;
                      }
                      router.refresh();
                    });
                  }}
                >
                  {isMarking ? "Marking…" : "Mark workout"}
                </Button>
                {markError && <p className="mt-3 text-sm text-destructive" role="alert">{markError}</p>}
              </div>
            )}
          </div>
        </section>
      </div>
      <section aria-labelledby="recent-workouts-heading" className="space-y-4">
        <h2 id="recent-workouts-heading" className="text-xl font-semibold">Recent workouts</h2>
        {visits.length ? <GymVisitsList visits={visits.slice(0, 3)} /> : (
          <div className="rounded-2xl border border-dashed border-border bg-card p-6">
            <p className="font-semibold">No workouts yet.</p>
            <p className="mt-2 text-sm text-muted-foreground">Mark a workout completed when you&apos;re ready.</p>
          </div>
        )}
      </section>
    </div>
  );
}
