import { ArrowRight } from "lucide-react";
import Link from "next/link";
import type { ReactNode } from "react";
import PulseCard from "@/app/components/design-system/pulse-card";
import ActivityBreakdown from "@/app/dashboard/activity-breakdown";

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

const number = new Intl.NumberFormat("en-US", { maximumFractionDigits: 2 });
const rowClass = "flex min-h-27 items-center gap-4 p-5";
const iconClass = "grid size-14 shrink-0 place-items-center rounded-[18px]";
const valueClass = "text-[clamp(1.6rem,2.6vw,2rem)] leading-[1.1] tracking-[-.04em] tabular-nums";
const unitClass = "ml-1 text-[.5em] font-bold tracking-normal text-muted-foreground";
const labelClass = "mt-0.5 font-semibold text-muted-foreground";

function FitnessSection({ children, busy }: { children: ReactNode; busy?: boolean }) {
  return (
    <section aria-labelledby="dashboard-fitness-heading" aria-busy={busy} className="grid gap-5">
      <header className="flex min-h-11 items-center justify-between gap-4">
        <h2 id="dashboard-fitness-heading" className="text-[clamp(1.4rem,2.4vw,1.8rem)] leading-[1.15] font-extrabold tracking-[-.035em]">
          Today at a glance
        </h2>
        <Link
          href="/fitness"
          aria-label="View all fitness"
          className="group -mr-3.5 inline-flex min-h-11 shrink-0 items-center gap-2 rounded-full px-3.5 font-bold text-muted-foreground transition-colors hover:bg-(--dashboard-activity-chip) hover:text-foreground"
        >
          View all <ArrowRight className="size-5 motion-safe:transition-transform motion-safe:group-hover:translate-x-[3px]" aria-hidden="true" />
        </Link>
      </header>
      <div className="grid grid-cols-1 gap-5 min-[561px]:grid-cols-2 min-[1051px]:grid-cols-4">
        {children}
      </div>
    </section>
  );
}

function MetricCard({ icon, color, label, value, progress }: {
  icon: ReactNode;
  color: string;
  label: string;
  value: ReactNode;
  progress?: ReactNode;
}) {
  return (
    <PulseCard className={`${rowClass} shadow-(--dashboard-activity-shadow)`}>
      <span className={`${iconClass} ${color} bg-[color-mix(in_srgb,currentColor_16%,transparent)]`} aria-hidden="true">
        <svg viewBox="0 0 24 24" className="size-7" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
          {icon}
        </svg>
      </span>
      <dl className="flex min-w-0 flex-1 flex-col">
        <dt className={`order-1 ${labelClass}`}>{label}</dt>
        <dd className={`${valueClass} ${value == null ? "font-semibold text-muted-foreground" : "font-extrabold"}`}>
          {value ?? <><span aria-hidden="true">—</span><span className="sr-only">No data</span></>}
        </dd>
        {progress && <dd className="order-2 mt-2">{progress}</dd>}
      </dl>
    </PulseCard>
  );
}

function StepProgress({ steps, goal }: { steps: number; goal: number }) {
  const percent = Math.max(0, Math.min(100, 100 * steps / goal));
  return (
    <div role="img" aria-label={`${Math.round(percent)} percent of the ${number.format(goal)} step goal`}
      className="h-1.5 overflow-hidden rounded-full bg-(--dashboard-activity-chip)">
      <div className="h-full rounded-full bg-linear-to-r from-(--brand-cyan) to-(--brand-blue)"
        style={{ width: `${percent}%` }} />
    </div>
  );
}

function ActivityRings({ rings }: { rings: Ring[] }) {
  return (
    <svg viewBox="0 0 56 56" className="size-14 shrink-0" aria-hidden="true">
      {rings.map(({ label, value, goal, color }, index) => {
        const radius = 22 - index * 6;
        const progress = value !== null && goal !== null && goal > 0 ? Math.min(1, value / goal) : 0;
        return (
          <g key={label} className={color} fill="none" stroke="currentColor" strokeWidth={5} strokeLinecap="round">
            <circle cx={28} cy={28} r={radius} opacity={0.22} />
            {progress > 0 && (
              <circle cx={28} cy={28} r={radius} pathLength={1}
                strokeDasharray={progress < 1 ? `${progress} 1` : undefined} transform="rotate(-90 28 28)" />
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
    <PulseCard className="relative shadow-(--dashboard-activity-shadow)">
      <ActivityBreakdown
        className={`${rowClass} w-full rounded-card text-left`}
        breakdown={(
          <dl className="grid gap-2 rounded-card border border-border bg-card p-4 text-sm shadow-lg">
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
        )}
      >
        <ActivityRings rings={rings} />
        <span className="min-w-0">
          <span className="block text-2xl leading-[1.1] font-extrabold tracking-[-.04em]">Activity</span>
          <span className={`block ${labelClass}`}>
            {!hasActivity ? "No data" : goals.length ? `${achieved} of 3 rings closed` : "No goals set"}
          </span>
        </span>
      </ActivityBreakdown>
    </PulseCard>
  );
}

function SleepValue({ seconds }: { seconds: number }) {
  const minutes = Math.floor(seconds / 60);
  return <>{Math.floor(minutes / 60)}<small className={unitClass}>h</small>{" "}{minutes % 60}<small className={unitClass}>min</small></>;
}

export function FitnessAtAGlance({ data, stepTarget }: { data: FitnessToday | null; stepTarget: number | null }) {
  const rings: Ring[] = [
    { label: "Move", value: data?.move_calories ?? null, goal: data?.move_calories_goal ?? null, unit: "kcal", color: "text-(--brand-pink)" },
    { label: "Exercise", value: data?.exercise_minutes ?? null, goal: data?.exercise_minutes_goal ?? null, unit: "min", color: "text-(--brand-gold)" },
    { label: "Stand", value: data?.stand_hours ?? null, goal: data?.stand_hours_goal ?? null, unit: "h", color: "text-(--brand-cyan)" },
  ];
  const steps = data?.steps_count;
  const active = data?.move_calories;

  return (
    <FitnessSection>
      <MetricCard
        icon={<><path d="M8 4c2 0 3 2 3 4s-1 3-2.5 3S6 9 6 7s0-3 2-3ZM7.5 14h3.5v2.5a1.8 1.8 0 0 1-3.5 0Z" /><path d="M16 8c2 0 3 2 3 4s-1 3-2.5 3S14 13 14 11s0-3 2-3ZM15.5 18h3.5v1a1.8 1.8 0 0 1-3.5 0Z" /></>}
        color="text-(--brand-purple-bright)"
        label="Steps"
        value={steps == null ? null : number.format(steps)}
        progress={steps != null && stepTarget != null && stepTarget > 0 ? <StepProgress steps={steps} goal={stepTarget} /> : undefined}
      />
      <MetricCard
        icon={<path d="M12 3c1 3.5 5 5.5 5 10a5 5 0 0 1-10 0c0-2 1-3 2-4 .3 1.2 1 2 2 2.2C10.5 8.5 11 5.5 12 3Z" />}
        color="text-(--brand-orange)"
        label="Active"
        value={active == null ? null : <>{number.format(active)}<small className={unitClass}>kcal</small></>}
      />
      <MetricCard
        icon={<path d="M20 14.5A8 8 0 0 1 9.5 4 8 8 0 1 0 20 14.5Z" />}
        color="text-[#8b6cf0]"
        label="Sleep"
        value={data?.total_asleep_seconds == null ? null : <SleepValue seconds={data.total_asleep_seconds} />}
      />
      <ActivityCard rings={rings} />
    </FitnessSection>
  );
}

export function FitnessAtAGlanceSkeleton() {
  return (
    <FitnessSection busy>
      {[0, 1, 2, 3].map((index) => (
        <PulseCard key={index} className={`${rowClass} shadow-(--dashboard-activity-shadow)`} aria-hidden="true">
          <span className={`${iconClass} bg-(--dashboard-activity-chip)`} />
          <span className="h-9 w-16 rounded-lg bg-(--dashboard-activity-chip)" />
        </PulseCard>
      ))}
    </FitnessSection>
  );
}
