import PulseCard from "@/app/components/design-system/pulse-card";
import Link from "next/link";
import { useId } from "react";

const workoutCtaClass =
  "inline-flex items-center justify-center max-w-full min-h-12 mt-3.5 py-2.5 px-[clamp(16px,4cqi,24px)] rounded-full " +
  "[background:var(--dashboard-fitness-action)] text-primary-foreground font-semibold text-center " +
  "shadow-(--dashboard-fitness-action-shadow) hover:brightness-95 dark:shadow-none";

const workoutDialClass =
  "block flex-none w-[clamp(104px,32cqi,190px)] max-w-full aspect-square rounded-full bg-card " +
  "shadow-(--dashboard-fitness-dial-shadow) dark:shadow-none";

interface GymVisitCardProps {
  visited: boolean | null;
}

export default function GymVisitCard({ visited }: GymVisitCardProps) {
  const gradientId = useId();
  const title = visited === null
    ? "Workout status unavailable."
    : visited
      ? "Workout recorded."
      : "No workout recorded today.";

  return (
    <PulseCard as="article" className="@container grid items-center bg-(--dashboard-fitness-surface) text-card-foreground" aria-label="Fitness today">
      <div className="flex items-center justify-center gap-[clamp(16px,5cqi,36px)] p-[clamp(20px,5cqi,32px)] @max-[20rem]:flex-col @max-[20rem]:text-center">
        <svg
          className={workoutDialClass}
          viewBox="0 0 160 160"
          aria-hidden="true"
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
            r="66"
            fill="none"
            stroke={visited ? `url(#${gradientId})` : undefined}
            strokeWidth="10"
            strokeLinecap="round"
            strokeDasharray={visited ? undefined : "3 16"}
          />
          <path
            className="stroke-brand-blue dark:stroke-brand-cyan"
            d="M42 80h19l9-18 14 36 10-18h24"
            fill="none"
            strokeWidth="5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>

        <div className="min-w-0 wrap-anywhere">
          <h2 className="text-[clamp(1.3rem,5.1cqi,2rem)] leading-[1.1] font-extrabold tracking-[-.025em]">{title}</h2>
          {visited === false && <p className="mt-1.5 text-muted-foreground">A short one still counts.</p>}
          <Link className={workoutCtaClass} href="/fitness">
            {visited === false ? "Get moving" : "View fitness"}
          </Link>
        </div>
      </div>
    </PulseCard>
  );
}
