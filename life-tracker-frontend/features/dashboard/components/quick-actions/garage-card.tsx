import { CarFront } from "lucide-react";
import QuickActionCard from "@/features/dashboard/components/quick-actions/quick-action-card";

export default function GarageCard() {
  return (
    <QuickActionCard
      tone="garage"
      href="/garage"
      title="Garage"
      description="Vehicles, fuel & maintenance"
      icon={<CarFront strokeWidth={1.8} />}
      decor={
        <svg viewBox="0 0 120 70" className="block h-auto w-16 @xs:w-20 @sm:w-24 @md:w-28 dashboard-compact:w-16">
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
            className="transform-fill origin-bottom motion-safe:animate-pulse-sweep"
          />
          <circle cx={60} cy={62} r={6} fill="currentColor" />
        </svg>
      }
    />
  );
}
