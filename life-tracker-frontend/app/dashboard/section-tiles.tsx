import type { ReactNode } from "react";
import { ArrowRight, CarFront, Utensils } from "lucide-react";
import { PulseCardLink } from "@/app/components/design-system/pulse-card";
import MealPlanWeekStrip from "./meal-plan-week-strip";

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
    <PulseCardLink href={href} className={`pulse-space-link ${className}`}>
      <span className="pulse-space-icon" aria-hidden="true">{icon}</span>
      <span className="pulse-space-copy">
        <strong>{title}</strong>
        <span>{description}</span>
      </span>
      <span className="pulse-space-decor" aria-hidden="true">{decor}</span>
      <span className="pulse-space-arrow" aria-hidden="true">
        <ArrowRight size={22} strokeWidth={2.4} />
      </span>
    </PulseCardLink>
  );
}

function GarageGauge() {
  return (
    <svg viewBox="0 0 120 70" className="pulse-garage-gauge">
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
        className="pulse-garage-needle"
      />
      <circle cx={60} cy={62} r={6} fill="currentColor" />
    </svg>
  );
}

export default function SectionTiles() {
  return (
    <nav className="pulse-spaces" aria-label="Your spaces">
      <SectionTile
        href="/garage"
        title="Garage"
        description="Vehicles, fuel & maintenance"
        icon={<CarFront strokeWidth={1.8} />}
        decor={<GarageGauge />}
        className="pulse-garage-link"
      />
      <SectionTile
        href="/meal-plan"
        title="Meal Plan"
        description="Plan your week and track meals eaten"
        icon={<Utensils strokeWidth={1.8} />}
        decor={<MealPlanWeekStrip />}
        className="pulse-meal-plan-link"
      />
    </nav>
  );
}
