"use client";

import { LayoutDashboard } from "lucide-react";
import PulseCard from "@/components/ui/pulse-card";
import PulseChoiceGroup from "@/components/ui/pulse-choice-group";
import { setDashboardView, useDashboardView } from "@/features/dashboard";
import { sectionClass, sectionDescriptionClass, sectionHeadingClass } from "@/features/profile/styles/profile-classes";

const options = [
  { value: "large", label: "Large" },
  { value: "compact", label: "Compact" },
];

export default function DashboardViewSettings() {
  const view = useDashboardView() ?? "large";
  return (
    <section className={sectionClass} aria-labelledby="profile-dashboard-view">
      <header>
        <h2 id="profile-dashboard-view" className={sectionHeadingClass}>
          <LayoutDashboard aria-hidden="true" />Dashboard view
        </h2>
        <p className={sectionDescriptionClass}>Choose how much space the home dashboard uses in this browser.</p>
      </header>
      <PulseCard padding="md">
        <PulseChoiceGroup label="Dashboard view" options={options} value={view} onChange={setDashboardView} columns />
      </PulseCard>
    </section>
  );
}
