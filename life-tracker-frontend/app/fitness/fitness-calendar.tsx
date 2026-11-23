"use client";

import { useState, useTransition, type CSSProperties } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  Check, ChevronLeft, ChevronRight, Clock, Dumbbell, Flame, List, Sun, Timer, Weight,
} from "lucide-react";
import Button from "@/app/components/design-system/button";
import ConfirmationDialog from "@/app/components/design-system/confirmation-dialog";
import PulseCard from "@/app/components/design-system/pulse-card";
import GymVisitsList, { type WorkoutGroup } from "@/app/fitness/gym-visits-list";
import { calendarDays, shiftMonth } from "@/app/fitness/calendar-utils.mjs";
import { markGymVisitAction } from "@/app/fitness/actions";
import { fitnessStyles as styles } from "@/app/fitness/fitness-classes";

const monthFormatter = new Intl.DateTimeFormat("en-IN", { month: "long", timeZone: "UTC" });
const dayFormatter = new Intl.DateTimeFormat("en-IN", { dateStyle: "full", timeZone: "UTC" });
const selectedDayFormatter = new Intl.DateTimeFormat("en-IN", {
  weekday: "short", day: "numeric", month: "short", timeZone: "UTC",
});
const weekdays = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

const statIcons = {
  workouts: { icon: Check, tone: "bg-brand-blue text-white" },
  trained: { icon: Timer, tone: "bg-brand-cyan text-on-cyan" },
  volume: { icon: Weight, tone: "bg-brand-purple-bright text-white" },
  sets: { icon: List, tone: "bg-brand-magenta text-white" },
  preference: { icon: Sun, tone: "bg-brand-gold text-on-gold" },
};

type FitnessStat = { id: string; label: string; value: string; subtitle: string; tooltip?: string };

export type FitnessOverview = {
  today: string;
  cards: FitnessStat[];
  calendar_workouts: Record<string, WorkoutGroup>;
  recent_workouts: WorkoutGroup[];
};

function parseDate(date: string) {
  return new Date(`${date}T00:00:00Z`);
}

function FitnessSummary({ last, streak, today, workouts }: {
  last?: FitnessStat;
  streak?: FitnessStat;
  today: string;
  workouts: FitnessOverview["calendar_workouts"];
}) {
  return (
    <div className="grid min-w-0 gap-4 sm:grid-cols-2 lg:grid-cols-1 lg:grid-rows-2 lg:gap-6">
      {last && (
        <section className={`${styles.last} relative isolate flex min-h-40 flex-col justify-between gap-4 overflow-hidden rounded-4xl p-5 text-white sm:p-6`}>
          <h2 className="flex items-center gap-2.5 font-semibold"><Clock size={21} aria-hidden="true" />{last.label}</h2>
          <div>
            <p className={styles.summaryValue}>{last.value}</p>
            {Object.keys(workouts).length > 0 && <p className="mt-1">{last.subtitle}</p>}
          </div>
          <Dumbbell aria-hidden="true" className="pointer-events-none absolute -right-5 -bottom-7 -z-10 size-40 -rotate-12 text-white/20" strokeWidth={1.4} />
        </section>
      )}
      {streak && (
        <PulseCard as="section" className={`${styles.surface} flex flex-col justify-between gap-3 p-5 sm:p-6`}>
          <h2 className={`${styles.muted} flex items-center gap-2.5 font-semibold`}><Flame size={21} aria-hidden="true" />{streak.label}</h2>
          <div>
            <p className={styles.summaryValue}>{streak.value}</p>
            <p className={`${styles.muted} mt-1`}>{streak.subtitle}</p>
          </div>
          <ul aria-label="Workouts in the last seven days" className="flex flex-wrap gap-1.5 sm:gap-2">
            {Array.from({ length: 7 }, (_, index) => {
              const date = parseDate(today);
              date.setUTCDate(date.getUTCDate() + index - 6);
              const hasWorkout = (workouts[date.toISOString().slice(0, 10)]?.workouts.length ?? 0) > 0;
              return (
                <li key={index} className={`${styles.streakDay} grid size-7 place-items-center rounded-full text-xs font-bold sm:size-8`} data-workout={hasWorkout}>
                  <span aria-hidden="true">{weekdays[(date.getUTCDay() + 6) % 7][0]}</span>
                  <span className="sr-only">{dayFormatter.format(date)}, {hasWorkout ? "workout logged" : "no workout logged"}</span>
                </li>
              );
            })}
          </ul>
        </PulseCard>
      )}
    </div>
  );
}

function FitnessStats({ cards }: { cards: FitnessStat[] }) {
  if (!cards.length) return null;

  return (
    <section aria-labelledby="fitness-stats-heading">
      <h2 id="fitness-stats-heading" className={styles.sectionTitle}>Your stats</h2>
      <ul className={styles.tileGrid}>
        {cards.map((card) => {
          const icon = statIcons[card.id as keyof typeof statIcons];
          const Icon = icon?.icon ?? List;
          const attendance = card.id === "attendance" ? Number.parseFloat(card.value) : NaN;
          return (
            <PulseCard as="li" key={card.id} className={`${styles.surface} flex items-center gap-4 p-4 sm:p-5`}>
              {Number.isFinite(attendance) ? (
                <span aria-hidden="true" className={styles.attendance} style={{ "--attendance": `${attendance}%` } as CSSProperties} />
              ) : (
                <span aria-hidden="true" className={`grid size-12 shrink-0 place-items-center rounded-2xl sm:size-14 ${icon?.tone ?? statIcons.workouts.tone}`}>
                  <Icon size={25} />
                </span>
              )}
              <div className="min-w-0">
                <h3 className={`${styles.muted} text-sm font-semibold`}>{card.label}</h3>
                <p className="mt-0.5 text-xl font-extrabold tracking-tight tabular-nums wrap-anywhere sm:text-2xl">{card.value}</p>
                <p className={`${styles.muted} mt-0.5 text-xs`}>{card.subtitle}{card.tooltip && ` · ${card.tooltip}`}</p>
              </div>
            </PulseCard>
          );
        })}
      </ul>
    </section>
  );
}

export default function FitnessCalendar({ overview }: { overview: FitnessOverview }) {
  const { today, cards, calendar_workouts: workoutsByDate, recent_workouts: recentWorkouts } = overview;
  const [selectedDate, setSelectedDate] = useState(today);
  const [isMarking, startMarking] = useTransition();
  const [markError, setMarkError] = useState("");
  const [dateToMark, setDateToMark] = useState<string | null>(null);
  const router = useRouter();
  const month = selectedDate.slice(0, 7);
  const selectedGroup = workoutsByDate[selectedDate];
  const day = parseDate(selectedDate);
  const last = cards.find(({ id }) => id === "last_workout");
  const streak = cards.find(({ id }) => id === "streak");
  const yearCards = cards.filter(({ id, value }) => id !== "last_workout" && id !== "streak" && value !== "Not available");

  function navigateMonth(offset: number) {
    const next = shiftMonth(month, offset);
    setSelectedDate(next === today.slice(0, 7) ? today : `${next}-01`);
  }

  function openLog(date: string) {
    setMarkError("");
    setDateToMark(date);
  }

  return (
    <div className="grid gap-6 sm:gap-8">
      <section aria-labelledby="fitness-title" className={styles.hero}>
        <div className="relative">
          <h1 id="fitness-title" className={styles.title}>Fitness</h1>
          <p className="mt-2 text-base font-medium sm:text-xl">Workouts, streaks &amp; stats</p>
          <Button
            type="button"
            className="mt-5 min-h-[clamp(2.75rem,4vw,3.5rem)] px-[clamp(1rem,2vw,1.5rem)] focus-visible:outline-white"
            shape="pill"
            variant="onHero"
            size="lg"
            icon={<Dumbbell size={20} aria-hidden="true" />}
            disabled={isMarking}
            onClick={() => openLog(selectedDate <= today ? selectedDate : today)}
          >
            Log gym visit
          </Button>
        </div>
        <div className={styles.ecg} aria-hidden="true">
          {["motion-safe:[animation-delay:-4s]", "motion-safe:[animation-delay:-2s]", ""].map((delay) => (
            <span key={delay} className={`${styles.ripple} ${delay}`} />
          ))}
          <svg viewBox="0 0 200 100" fill="none">
            <path d="M0 50H72L84 18L100 82L112 34L122 50H200" pathLength="1" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </div>
      </section>

      <div className="grid gap-4 lg:grid-cols-[minmax(0,1.2fr)_minmax(0,1fr)] lg:gap-6">
        <PulseCard as="section" aria-label="Workout calendar" className={`${styles.surface} min-w-0 p-3 sm:p-6`}>
          <div className="mb-4 flex items-center justify-between gap-3">
            <h2 aria-live="polite" className="text-2xl leading-none font-extrabold tracking-tight sm:text-3xl">
              {monthFormatter.format(parseDate(`${month}-01`))}
              <span className={`${styles.muted} mt-1 block text-sm font-semibold tracking-normal`}>{month.slice(0, 4)}</span>
            </h2>
            <div className="flex gap-2">
              <Button type="button" variant="iconSecondary" size="lg" shape="pill" aria-label="Previous month" onClick={() => navigateMonth(-1)}>
                <ChevronLeft size={20} aria-hidden="true" />
              </Button>
              <Button type="button" variant="iconSecondary" size="lg" shape="pill" aria-label="Next month" onClick={() => navigateMonth(1)}>
                <ChevronRight size={20} aria-hidden="true" />
              </Button>
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
          <div aria-live="polite" className={`${styles.selectedDate} mt-4 flex flex-wrap items-center justify-between gap-3 rounded-3xl py-3 pr-3.5 pl-4.5`}>
            <div>
              <h3 className="font-extrabold"><time dateTime={selectedDate}>{selectedDayFormatter.format(day)}</time></h3>
              {selectedGroup ? (
                <ul className={styles.selectedDateDetails}>
                  {selectedGroup.workouts.map((visit) => (
                    <li key={visit.id}>
                      <Link href={`/fitness/${visit.id}`} className="underline-offset-2 hover:underline focus-visible:underline">
                        Gym visit at {visit.time_label}
                      </Link>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className={styles.selectedDateDetails}>No workout logged</p>
              )}
            </div>
            {!selectedGroup && selectedDate <= today && (
              <Button type="button" size="lg" shape="pill" variant="gradient" className="[--button-fill:var(--fitness-gradient)] [--button-foreground:white]" disabled={isMarking} onClick={() => openLog(selectedDate)}>
                Log visit
              </Button>
            )}
          </div>
        </PulseCard>
        <FitnessSummary last={last} streak={streak} today={today} workouts={workoutsByDate} />
      </div>

      <section aria-labelledby="recent-workouts-heading">
        <h2 id="recent-workouts-heading" className={styles.sectionTitle}>Recent workouts</h2>
        {recentWorkouts.length ? <GymVisitsList groups={recentWorkouts} /> : (
          <p className={styles.empty}>No workouts yet.</p>
        )}
      </section>

      <FitnessStats cards={yearCards} />

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
            setSelectedDate(dateToMark);
            setDateToMark(null);
            router.refresh();
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
