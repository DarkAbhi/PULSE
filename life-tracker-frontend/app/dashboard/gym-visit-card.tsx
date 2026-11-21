import PulseCard from "@/app/components/design-system/pulse-card";
import Link from "next/link";
import { useId } from "react";

interface GymVisitCardProps {
  visited: boolean;
}

export default function GymVisitCard({ visited }: GymVisitCardProps) {
  const gradientId = useId();

  return (
    <PulseCard as="article" className="pulse-workout-card" aria-label="Fitness today">
      <div className="pulse-workout-layout">
        <svg
          className="pulse-workout-dial"
          viewBox="0 0 160 160"
          aria-hidden="true"
        >
          <defs>
            <linearGradient id={gradientId} x2="100%" y2="100%">
              <stop offset="0%" stopColor="#19d3f3" />
              <stop offset="50%" stopColor="#4c6bff" />
              <stop offset="100%" stopColor="#ff2fd0" />
            </linearGradient>
          </defs>
          <circle
            className={visited ? undefined : "pulse-workout-track"}
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
            className="pulse-workout-heartbeat"
            d="M42 80h19l9-18 14 36 10-18h24"
            fill="none"
            strokeWidth="5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>

        <div className="pulse-workout-copy">
          <h2>{visited ? "Workout recorded." : "No workout recorded today."}</h2>
          {!visited && <p>A short one still counts.</p>}
          <Link className="pulse-workout-cta" href="/fitness">
            {visited ? "View fitness" : "Get moving"}
          </Link>
        </div>
      </div>
    </PulseCard>
  );
}
