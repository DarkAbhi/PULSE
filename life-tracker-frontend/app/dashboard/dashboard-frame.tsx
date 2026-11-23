"use client";

import type { ReactNode } from "react";
import { useDashboardView } from "@/app/lib/dashboard-view";

export default function DashboardFrame({ children, className }: { children: ReactNode; className: string }) {
  const view = useDashboardView();
  return <main id="main-content" tabIndex={-1} className={className} data-dashboard-view={view ?? "initial"}>{children}</main>;
}
