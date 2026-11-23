import { ArrowRight, Flame, Footprints, Moon } from "lucide-react";
import Link from "next/link";
import type { ReactNode } from "react";
import PulseCard from "@/app/components/design-system/pulse-card";

export type FitnessToday = {
  date: string;
  steps_count: number | null;
  move_calories: number | null;
  move_calories_goal: number | null;
  exercise_minutes: number | null;
  exercise_minutes_goal: number | null;
  stand_hours: number | null;
  stand_hours_goal: number | null;
  total_asleep_seconds: number | null;
};

type Ring = {
  label: string;
  value: number | null;
  goal: number | null;
  unit: string;
  color: string;
};

const number = new Intl.NumberFormat("en", { maximumFractionDigits: 2 });
const gridClass = "grid grid-cols-1 gap-3 min-[28rem]:grid-cols-2 xl:grid-cols-4";
const rowClass = "flex min-h-23 items-center gap-3 px-4 py-3 lg:min-h-27 lg:gap-4 dashboard-compact:lg:min-h-23";
const surfaceClass = "bg-(--dashboard-fitness-surface)";
const iconClass =
  "grid size-12 shrink-0 place-items-center rounded-full bg-(--dashboard-metric-icon-surface) " +
  "text-(--dashboard-metric-icon) lg:size-15 dashboard-compact:lg:size-12";
const valueClass = "text-xl leading-tight font-extrabold tracking-tight tabular-nums lg:text-2xl";

function FitnessSection({ children, busy }: { children: ReactNode; busy?: boolean }) {
  return (
    <section aria-labelledby="dashboard-fitness-heading" aria-busy={busy} className="grid gap-3">
      <div className="flex flex-wrap items-center justify-between gap-x-3">
        <h2 id="dashboard-fitness-heading" className="text-xl leading-tight font-extrabold tracking-tight lg:text-2xl">
          Today at a glance
        </h2>
        <Link
          href="/fitness"
          aria-label="View all fitness"
          className="inline-flex min-h-11 items-center gap-1.5 rounded-md text-sm font-semibold text-muted-foreground hover:text-foreground"
        >
          View all <ArrowRight className="size-4" aria-hidden="true" />
        </Link>
      </div>
      <div className={gridClass}>{children}</div>
    </section>
  );
}

function MetricCard({ icon, label, value }: { icon: ReactNode; label: string; value: ReactNode }) {
  return (
    <PulseCard className={`${surfaceClass} ${rowClass}`}>
      <span className={`${iconClass} [&>svg]:size-6`} aria-hidden="true">{icon}</span>
      <dl className="flex min-w-0 flex-col gap-0.5">
        <dt className="order-last text-sm font-medium text-muted-foreground">{label}</dt>
        <dd className={valueClass}>
          {value ?? <><span aria-hidden="true">—</span><span className="sr-only">No data</span></>}
        </dd>
      </dl>
    </PulseCard>
  );
}

function ActivityRings({ rings }: { rings: Ring[] }) {
  return (
    <svg viewBox="0 0 52 52" className="size-full" aria-hidden="true">
      {rings.map(({ label, value, goal, color }, index) => {
        const radius = 22 - index * 6;
        const progress = value !== null && goal !== null && goal > 0 ? value / goal : 0;
        // Completed laps share the same track. Draw only the last partial lap on top.
        const lastLap = progress % 1;
        return (
          <g key={label} className={color} fill="none" stroke="currentColor" strokeWidth={4.5}>
            <circle cx={26} cy={26} r={radius} opacity={0.24} />
            {progress >= 1 && <circle cx={26} cy={26} r={radius} />}
            {lastLap > 0 && (
              <circle
                cx={26}
                cy={26}
                r={radius}
                pathLength={1}
                strokeDasharray={`${lastLap} 1`}
                strokeLinecap="round"
                transform="rotate(-90 26 26)"
                className={progress > 1 ? "brightness-125" : undefined}
              />
            )}
          </g>
        );
      })}
    </svg>
  );
}

function ActivityCard({ rings }: { rings: Ring[] }) {
  const goals = rings.filter(({ goal }) => goal !== null && goal > 0);
  const achieved = goals.filter(({ value, goal }) => value !== null && goal !== null && value >= goal).length;
  const hasActivity = rings.some(({ value }) => value !== null);

  return (
    <PulseCard className={`relative ${surfaceClass}`}>
      <details>
        <summary className={`${rowClass} cursor-pointer list-none rounded-card hover:bg-foreground/5 focus-visible:outline-2 focus-visible:outline-ring [&::-webkit-details-marker]:hidden`}>
          <span className={`${iconClass} p-1.5`}><ActivityRings rings={rings} /></span>
          <span className="min-w-0">
            <span className="block text-lg leading-tight font-extrabold tracking-tight lg:text-xl">Activity</span>
            <span className="mt-0.5 block text-sm font-medium text-muted-foreground">
              {goals.length > 0 ? (
                <><strong className="font-bold text-foreground">{achieved}</strong> / {goals.length} goals</>
              ) : hasActivity ? "No goals set" : "No data"}
            </span>
          </span>
        </summary>
        <dl className="absolute inset-x-0 bottom-full z-10 mb-2 grid gap-2 rounded-card border border-border bg-card p-4 text-sm shadow-lg">
          {rings.map(({ label, value, goal, unit, color }) => (
            <div key={label} className="flex flex-wrap items-baseline justify-between gap-x-2">
              <dt className="flex items-center gap-2 font-semibold">
                <span aria-hidden="true" className={`size-2 rounded-full bg-current ${color}`} />
                {label}
              </dt>
              <dd className="text-right text-muted-foreground tabular-nums">
                {value === null ? "No data" : `${number.format(value)}${goal ? ` / ${number.format(goal)}` : ""} ${unit}`}
              </dd>
            </div>
          ))}
        </dl>
      </details>
    </PulseCard>
  );
}

function formatSleep(seconds: number) {
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  return hours > 0 ? `${hours}h ${minutes % 60}m` : `${minutes}m`;
}

export function FitnessAtAGlance({ data }: { data: FitnessToday | null }) {
  const rings: Ring[] = [
    { label: "Move", value: data?.move_calories ?? null, goal: data?.move_calories_goal ?? null, unit: "kcal", color: "text-(--dashboard-ring-move)" },
    { label: "Exercise", value: data?.exercise_minutes ?? null, goal: data?.exercise_minutes_goal ?? null, unit: "min", color: "text-(--dashboard-ring-exercise)" },
    { label: "Stand", value: data?.stand_hours ?? null, goal: data?.stand_hours_goal ?? null, unit: "h", color: "text-(--dashboard-ring-stand)" },
  ];
  const active = data?.move_calories;

  return (
    <FitnessSection>
      <MetricCard icon={<Footprints />} label="Steps" value={data?.steps_count != null ? number.format(data.steps_count) : null} />
      <MetricCard
        icon={<Flame className="fill-current text-(--dashboard-metric-energy)" />}
        label="Active"
        value={active != null ? <>{number.format(active)} <span className="text-sm font-bold">kcal</span></> : null}
      />
      <MetricCard icon={<Moon className="fill-current" />} label="Sleep" value={data?.total_asleep_seconds != null ? formatSleep(data.total_asleep_seconds) : null} />
      <ActivityCard rings={rings} />
    </FitnessSection>
  );
}

export function FitnessAtAGlanceSkeleton() {
  return (
    <FitnessSection busy>
      {[0, 1, 2, 3].map((index) => (
        <PulseCard key={index} className={`${surfaceClass} ${rowClass}`} aria-hidden="true">
          <span className={iconClass} />
          <span className="h-9 w-16 rounded-lg bg-foreground/5" />
        </PulseCard>
      ))}
    </FitnessSection>
  );
}
