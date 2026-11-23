import type { ReactNode } from "react";
import { ArrowRight, CarFront, Utensils } from "lucide-react";
import { PulseCardLink } from "@/app/components/design-system/pulse-card";
import MealPlanWeekStrip from "@/app/dashboard/meal-plan-week-strip";

const spaceIconClass =
  "grid flex-none place-items-center aspect-square rounded-full w-[clamp(42px,5cqi,60px)] border-[1.5px] " +
  "border-white/40 bg-white/20 [&_svg]:w-[clamp(22px,2.5cqi,30px)] " +
  "[&_svg]:h-auto";

const spaceLinkClass =
  "flex items-center gap-[clamp(10px,1.25cqi,18px)] min-h-37.5 p-[clamp(16px,2.2cqi,30px)] " +
  "rounded-[clamp(28px,4cqi,40px)] [background:linear-gradient(135deg,var(--tile-start),var(--tile-end))] text-white " +
  "shadow-[0_20px_40px_-22px_var(--tile-start)] dark:shadow-[0_20px_40px_-24px_rgb(0_0_0_/_70%)] " +
  "motion-safe:[transition:transform_200ms_ease] motion-safe:active:[transform:scale(.985)]";

type SectionTileProps = {
  href: string;
  title: string;
  description: string;
  icon: ReactNode;
  decor: ReactNode;
  className: string;
};

function SectionTile({ href, title, description, icon, decor, className }: SectionTileProps) {
  return (
    <PulseCardLink href={href} className={`${spaceLinkClass} ${className}`}>
      <span className={spaceIconClass} aria-hidden="true">{icon}</span>
      <span className="min-w-0 flex-1 wrap-anywhere">
        <strong className="block text-[clamp(1.4rem,2.5cqi,2rem)] leading-[1.05] font-extrabold tracking-[-.03em]">{title}</strong>
        <span className="mt-1.5 block max-w-[26ch] text-[clamp(.875rem,1.5cqi,1rem)] leading-snug font-medium">{description}</span>
      </span>
      <span className="hidden flex-none @min-[66rem]:block" aria-hidden="true">{decor}</span>
      <span className="grid flex-none place-items-center aspect-square rounded-full w-[clamp(36px,4cqi,48px)] bg-white text-(--tile-start)" aria-hidden="true">
        <ArrowRight size={22} strokeWidth={2.4} />
      </span>
    </PulseCardLink>
  );
}

function GarageGauge() {
  return (
    <svg viewBox="0 0 120 70" className="w-30 h-17.5">
      <path
        d="M10 62A50 50 0 0 1 110 62"
        fill="none"
        stroke="currentColor"
        strokeOpacity={0.45}
        strokeWidth={7}
        strokeLinecap="round"
        strokeDasharray="2 10"
      />
      <line
        x1={60}
        y1={62}
        x2={60}
        y2={22}
        stroke="currentColor"
        strokeWidth={4}
        strokeLinecap="round"
        className="transform-view [transform-origin:60px_62px] @min-[66rem]:motion-safe:animate-pulse-sweep"
      />
      <circle cx={60} cy={62} r={6} fill="currentColor" />
    </svg>
  );
}

export default function SectionTiles() {
  return (
    <nav className="@container grid grid-cols-1 gap-4 md:grid-cols-2" aria-label="Your spaces">
      <SectionTile
        href="/garage"
        title="Garage"
        description="Vehicles, fuel & maintenance"
        icon={<CarFront strokeWidth={1.8} />}
        decor={<GarageGauge />}
        className="[--tile-start:#2440e0] [--tile-end:#0f75b3]"
      />
      <SectionTile
        href="/meal-plan"
        title="Meal Plan"
        description="Plan your week and track meals eaten"
        icon={<Utensils strokeWidth={1.8} />}
        decor={<MealPlanWeekStrip />}
        className="[--tile-start:#c91a8e] [--tile-end:#cf3c36]"
      />
    </nav>
  );
}
