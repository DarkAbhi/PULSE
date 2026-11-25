import { useId } from "react";
import { Dumbbell } from "lucide-react";
import QuickActionCard from "@/app/dashboard/quick-action-card";

const workoutDialClass =
  "block w-[clamp(64px,24cqi,112px)] aspect-square rounded-full bg-card " +
  "shadow-[0_10px_24px_-12px_rgb(0_0_0/45%)] dark:shadow-none dashboard-compact:w-14";

/** Dashed ring while today is empty; a full gradient ring once a workout is recorded. */
function WorkoutDial({ visited }: { visited: boolean | null }) {
  const gradientId = useId();
  return (
    <svg className={workoutDialClass} viewBox="0 0 160 160">
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

interface GymVisitCardProps {
  visited: boolean | null;
}

export default function GymVisitCard({ visited }: GymVisitCardProps) {
  const status = visited === null
    ? "Workout status unavailable."
    : visited
      ? "Workout recorded."
      : "No workout recorded today.";

  return (
    <QuickActionCard
      href="/fitness"
      title="Fitness"
      description={visited === false ? <>{status} A short one still counts.</> : status}
      icon={<Dumbbell strokeWidth={1.8} />}
      decor={<WorkoutDial visited={visited} />}
      className="[--tile-start:var(--dashboard-fitness-start)] [--tile-end:var(--dashboard-fitness-end)]"
    />
  );
}
