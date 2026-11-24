import type { ReactNode } from "react";
import { ArrowRight, CarFront, Utensils } from "lucide-react";
import { PulseCardLink } from "@/app/components/design-system/pulse-card";
import MealPlanWeekStrip from "@/app/dashboard/meal-plan-week-strip";

const spaceIconClass =
  "grid flex-none place-items-center aspect-square rounded-full w-[clamp(42px,5cqi,60px)] border-[1.5px] " +
  "border-white/40 bg-white/20 [&_svg]:w-[clamp(22px,2.5cqi,30px)] " +
  "[&_svg]:h-auto dashboard-compact:w-11 dashboard-compact:[&_svg]:w-6";

const spaceLinkClass =
  "flex items-center gap-[clamp(10px,1.25cqi,18px)] min-h-37.5 p-[clamp(16px,2.2cqi,30px)] " +
  "motion-safe:[transition:transform_200ms_ease] motion-safe:active:[transform:scale(.985)] " +
  "dashboard-compact:grid dashboard-compact:grid-cols-[auto_minmax(0,1fr)_auto] " +
  "dashboard-compact:content-center dashboard-compact:gap-3 dashboard-compact:min-h-36 dashboard-compact:p-4";

type SectionTileProps = {
  href: string;
  title: string;
  description: string;
  icon: ReactNode;
  decor: ReactNode;
  decorClassName?: string;
  className: string;
};

function SectionTile({ href, title, description, icon, decor, decorClassName = "", className }: SectionTileProps) {
  return (
    <PulseCardLink href={href} variant="tile" className={`${spaceLinkClass} ${className}`}>
      <span className={spaceIconClass} aria-hidden="true">{icon}</span>
      <span className="min-w-0 flex-1 wrap-anywhere">
        <strong className="block text-[clamp(1.4rem,2.5cqi,2rem)] leading-[1.05] font-extrabold tracking-[-.03em] dashboard-compact:text-xl">{title}</strong>
        <span className="mt-1.5 block max-w-[26ch] text-[clamp(.875rem,1.5cqi,1rem)] leading-snug font-medium dashboard-compact:mt-1 dashboard-compact:text-sm">{description}</span>
      </span>
      <span className={`hidden flex-none @min-[66rem]:block dashboard-compact:col-span-3 dashboard-compact:row-start-2 dashboard-compact:block ${decorClassName}`} aria-hidden="true">{decor}</span>
      <span className="grid flex-none place-items-center aspect-square rounded-full w-[clamp(36px,4cqi,48px)] bg-white text-(--tile-start) dashboard-compact:w-9" aria-hidden="true">
        <ArrowRight size={22} strokeWidth={2.4} className="dashboard-compact:size-5" />
      </span>
    </PulseCardLink>
  );
}

function GarageGauge() {
  return (
    <svg viewBox="0 0 120 70" className="w-30 h-17.5 dashboard-compact:h-auto dashboard-compact:w-21">
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
        className="transform-view [transform-origin:60px_62px] @min-[66rem]:motion-safe:animate-pulse-sweep dashboard-compact:motion-safe:animate-pulse-sweep"
      />
      <circle cx={60} cy={62} r={6} fill="currentColor" />
    </svg>
  );
}

export default function SectionTiles() {
  return (
    <nav className="grid grid-cols-1 gap-4 md:grid-cols-2 dashboard-compact:gap-3 dashboard-compact:xl:col-span-2 dashboard-compact:xl:grid-cols-subgrid dashboard-compact:xl:grid-rows-subgrid" aria-label="Your spaces">
      <SectionTile
        href="/garage"
        title="Garage"
        description="Vehicles, fuel & maintenance"
        icon={<CarFront strokeWidth={1.8} />}
        decor={<GarageGauge />}
        decorClassName="dashboard-compact:justify-self-end"
        className="[--tile-start:var(--dashboard-garage-start)] [--tile-end:var(--dashboard-garage-end)]"
      />
      <SectionTile
        href="/meal-plan"
        title="Meal Plan"
        description="Plan your week and track meals eaten"
        icon={<Utensils strokeWidth={1.8} />}
        decor={<MealPlanWeekStrip />}
        className="[--tile-start:var(--dashboard-meal-start)] [--tile-end:var(--dashboard-meal-end)]"
      />
    </nav>
  );
}
