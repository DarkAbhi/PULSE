import PulseCard from "@/app/components/design-system/pulse-card";
import Link from "next/link";
import { useId } from "react";

const workoutCtaClass =
  "inline-flex items-center justify-center max-w-full min-h-12 mt-3.5 py-2.5 px-[clamp(16px,4cqi,24px)] rounded-full " +
  "[background:linear-gradient(90deg,#404fce,#a31c83)] text-white font-semibold text-center " +
  "shadow-[0_12px_24px_-10px_rgb(255_47_208_/_60%)] hover:brightness-[.94] dark:[background:#f4f1ff] " +
  "dark:text-[#0d0a1f] dark:shadow-none";

const workoutDialClass =
  "block flex-none w-[clamp(104px,32cqi,190px)] max-w-full aspect-square rounded-full [background:white] " +
  "shadow-[0_18px_40px_-18px_rgb(76_107_255_/_35%)] dark:[background:#1a1636] dark:shadow-none";

interface GymVisitCardProps {
  visited: boolean;
}

export default function GymVisitCard({ visited }: GymVisitCardProps) {
  const gradientId = useId();

  return (
    <PulseCard as="article" className="@container grid items-center [background:#f6f5ff] text-[#1a1740] dark:[background:#211d3c] dark:text-white" aria-label="Fitness today">
      <div className="flex items-center justify-center gap-[clamp(16px,5cqi,36px)] p-[clamp(20px,5cqi,32px)] @max-[20rem]:flex-col @max-[20rem]:text-center">
        <svg
          className={workoutDialClass}
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
            className={visited ? undefined : "stroke-[rgb(26_23_64_/_15%)] dark:stroke-[rgb(244_241_255_/_20%)]"}
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
            className="stroke-[#4c6bff] dark:stroke-[#19d3f3]"
            d="M42 80h19l9-18 14 36 10-18h24"
            fill="none"
            strokeWidth="5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>

        <div className="min-w-0 wrap-anywhere">
          <h2 className="text-[clamp(1.3rem,5.1cqi,2rem)] leading-[1.1] font-extrabold tracking-[-.025em]">{visited ? "Workout recorded." : "No workout recorded today."}</h2>
          {!visited && <p className="mt-1.5 text-[#5e5a7d] dark:text-[#a49ec4]">A short one still counts.</p>}
          <Link className={workoutCtaClass} href="/fitness">
            {visited ? "View fitness" : "Get moving"}
          </Link>
        </div>
      </div>
    </PulseCard>
  );
}
