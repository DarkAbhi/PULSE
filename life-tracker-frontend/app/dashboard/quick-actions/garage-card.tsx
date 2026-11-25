import { CarFront } from "lucide-react";
import QuickActionCard from "@/app/dashboard/quick-actions/quick-action-card";

export default function GarageCard() {
  return (
    <QuickActionCard
      href="/garage"
      title="Garage"
      description="Vehicles, fuel & maintenance"
      icon={<CarFront strokeWidth={1.8} />}
      decor={
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
            className="transform-view origin-[60px_62px] motion-safe:animate-pulse-sweep"
          />
          <circle cx={60} cy={62} r={6} fill="currentColor" />
        </svg>
      }
      className="[--tile-start:var(--dashboard-garage-start)] [--tile-end:var(--dashboard-garage-end)]"
    />
  );
}
