"use client";

import { LayoutDashboard } from "lucide-react";
import PulseCard from "@/app/components/design-system/pulse-card";
import PulseChoiceGroup from "@/app/components/design-system/pulse-choice-group";
import { setDashboardView, useDashboardView } from "@/app/lib/dashboard-view";
import { sectionClass, sectionDescriptionClass, sectionHeadingClass } from "@/app/profile/profile-classes";

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
      <PulseCard className="p-6 shadow-card">
        <PulseChoiceGroup label="Dashboard view" options={options} value={view} onChange={setDashboardView} columns />
      </PulseCard>
    </section>
  );
}
