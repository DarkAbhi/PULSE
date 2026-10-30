import PulseCard from "../components/design-system/pulse-card";
import Link from "next/link";
import { Dumbbell, ArrowRight } from "lucide-react";

interface GymVisitCardProps {
  visited: boolean;
}

export default function GymVisitCard({ visited }: GymVisitCardProps) {
  return (
    <PulseCard as="article" className="pulse-summary-card">
      <span className="pulse-card-icon" aria-hidden="true"><Dumbbell className="h-6 w-6" /></span>
      <p className="pulse-card-label">Fitness · Today</p>
      <h2>{visited ? "Workout recorded" : "No workout recorded today"}</h2>
      <div className="pulse-card-footer">
        <Link className="pulse-card-cta" href="/fitness">{visited ? "View fitness" : "Get moving"}<ArrowRight className="h-4 w-4" /></Link>
        <img src="/dashboard/fitness.webp" width="240" height="160" alt="" className="pulse-card-art" />
      </div>
    </PulseCard>
  );
}
