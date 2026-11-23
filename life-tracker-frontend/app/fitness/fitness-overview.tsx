import "server-only";
import { Check, Clock, Dumbbell, Flame, List, Sun, Timer, Weight } from "lucide-react";
import type { CSSProperties } from "react";
import PulseCard from "@/app/components/design-system/pulse-card";
import GymVisitsList from "@/app/fitness/gym-visits-list";
import { fitnessStyles as styles } from "@/app/fitness/fitness-classes";
import type { FitnessOverview, FitnessStat } from "@/app/fitness/types";

const dayFormatter = new Intl.DateTimeFormat("en-IN", { dateStyle: "full", timeZone: "UTC" });
const weekdays = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

function parseDate(date: string) {
  return new Date(`${date}T00:00:00Z`);
}

const statIcons = {
  workouts: { icon: Check, tone: "bg-brand-blue text-white" },
  trained: { icon: Timer, tone: "bg-brand-cyan text-on-cyan" },
  volume: { icon: Weight, tone: "bg-brand-purple-bright text-white" },
  sets: { icon: List, tone: "bg-brand-magenta text-white" },
  preference: { icon: Sun, tone: "bg-brand-gold text-on-gold" },
};

export function FitnessSummary({ last, streak, today, workoutDates }: {
  last?: FitnessStat;
  streak?: FitnessStat;
  today: string;
  workoutDates: Set<string>;
}) {
  return (
    <div className="grid min-w-0 gap-4 sm:grid-cols-2 lg:grid-cols-1 lg:grid-rows-2 lg:gap-6">
      {last && (
        <section className={`${styles.last} relative isolate flex min-h-40 flex-col justify-between gap-4 overflow-hidden rounded-4xl p-5 text-white sm:p-6`}>
          <h2 className="flex items-center gap-2.5 font-semibold"><Clock size={21} aria-hidden="true" />{last.label}</h2>
          <div>
            <p className={styles.summaryValue}>{last.value}</p>
            {workoutDates.size > 0 && <p className="mt-1">{last.subtitle}</p>}
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
              const hasWorkout = workoutDates.has(date.toISOString().slice(0, 10));
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

export function FitnessStats({ cards }: { cards: FitnessStat[] }) {
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

export function RecentWorkouts({ recentWorkouts }: { recentWorkouts: FitnessOverview["recent_workouts"] }) {
  return (
      <section aria-labelledby="recent-workouts-heading">
        <h2 id="recent-workouts-heading" className={styles.sectionTitle}>Recent workouts</h2>
        {recentWorkouts.length ? <GymVisitsList groups={recentWorkouts} /> : (
          <p className={styles.empty}>No workouts yet.</p>
        )}
      </section>
  );
}
