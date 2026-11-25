import { CarFront, Utensils } from "lucide-react";
import QuickActionCard from "@/app/dashboard/quick-action-card";
import MealPlanWeekStrip from "@/app/dashboard/meal-plan-week-strip";

function GarageGauge() {
  return (
    <svg viewBox="0 0 120 70" className="block w-[clamp(72px,26cqi,120px)] h-auto dashboard-compact:w-18">
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
        className="transform-view [transform-origin:60px_62px] motion-safe:animate-pulse-sweep"
      />
      <circle cx={60} cy={62} r={6} fill="currentColor" />
    </svg>
  );
}

export default function SectionTiles() {
  return (
    <nav className="grid grid-cols-1 gap-4 md:grid-cols-2 dashboard-compact:gap-3 dashboard-compact:xl:col-span-2 dashboard-compact:xl:grid-cols-subgrid dashboard-compact:xl:grid-rows-subgrid" aria-label="Your spaces">
      <QuickActionCard
        href="/garage"
        title="Garage"
        description="Vehicles, fuel & maintenance"
        icon={<CarFront strokeWidth={1.8} />}
        decor={<GarageGauge />}
        className="[--tile-start:var(--dashboard-garage-start)] [--tile-end:var(--dashboard-garage-end)]"
      />
      <QuickActionCard
        href="/meal-plan"
        title="Meal Plan"
        description="Plan your week and track meals eaten"
        icon={<Utensils strokeWidth={1.8} />}
        footer={<MealPlanWeekStrip />}
        className="[--tile-start:var(--dashboard-meal-start)] [--tile-end:var(--dashboard-meal-end)]"
      />
    </nav>
  );
}
