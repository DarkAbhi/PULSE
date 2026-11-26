import { useId } from "react";
import { Dumbbell } from "lucide-react";
import QuickActionCard from "@/features/dashboard/components/quick-actions/quick-action-card";

function WorkoutDial({ visited }: { visited: boolean | null }) {
  const gradientId = useId();
  return (
    <svg
      className="block size-16 rounded-full bg-card shadow-tile-object @xs:size-20 @sm:size-24 @md:size-28 dark:shadow-none dashboard-compact:size-14"
      viewBox="0 0 160 160"
    >
      <defs>
        <linearGradient id={gradientId} x2="100%" y2="100%">
          <stop offset="0%" stopColor="var(--brand-cyan)" />
          <stop offset="50%" stopColor="var(--brand-blue)" />
          <stop offset="100%" stopColor="var(--brand-pink)" />
        </linearGradient>
      </defs>
      <circle
        className={visited ? undefined : "stroke-(--dashboard-fitness-ring)"}
        cx="80"
        cy="80"
        r="62"
        fill="none"
        stroke={visited ? `url(#${gradientId})` : undefined}
        strokeWidth="12"
        strokeLinecap="round"
        strokeDasharray={visited ? undefined : "3 16"}
      />
      <path
        className="stroke-brand-blue dark:stroke-brand-cyan"
        d="M42 80h19l9-18 14 36 10-18h24"
        fill="none"
        strokeWidth="6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export default async function FitnessCard({ workout }: { workout: Promise<{ visited: boolean } | null> }) {
  const visited = (await workout)?.visited ?? null;
  const status = visited === null
    ? "Workout status unavailable."
    : visited
      ? "Workout recorded."
      : "No workout recorded today.";

  return (
    <QuickActionCard
      tone="fitness"
      href="/fitness"
      title="Fitness"
      description={visited === false ? <>{status} A short one still counts.</> : status}
      icon={<Dumbbell strokeWidth={1.8} />}
      decor={<WorkoutDial visited={visited} />}
    />
  );
}
