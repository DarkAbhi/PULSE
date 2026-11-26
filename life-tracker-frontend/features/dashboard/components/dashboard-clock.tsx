"use client";

import { useEffect, useState } from "react";
import { useTimeZone } from "@/providers/profile-preferences-provider";

export default function DashboardClock() {
  const { timeZone } = useTimeZone();
  const [now, setNow] = useState<Date | null>(null);

  useEffect(() => {
    const update = () => setNow(new Date());
    update();
    const timer = window.setInterval(update, 1000);
    return () => window.clearInterval(timer);
  }, []);

  const time = now?.toLocaleTimeString("en-IN", {
    hour: "numeric",
    minute: "2-digit",
    hour12: true,
    timeZone,
  }).toUpperCase() ?? "—:—";
  const date = now?.toLocaleDateString("en-GB", {
    weekday: "long",
    day: "numeric",
    month: "short",
    year: "numeric",
    timeZone,
  }) ?? "Your day at a glance";

  return (
    <div className="min-w-0 dashboard-compact:sm:flex dashboard-compact:sm:items-center dashboard-compact:sm:gap-4">
      <p data-clock="time" className="text-2xl leading-[1.25] font-bold tabular-nums compact:text-[28px] dashboard-compact:text-xl dashboard-compact:sm:whitespace-nowrap">
        {time}
      </p>
      <p data-clock="date" className="mt-1 text-sm text-muted-foreground max-w-[24ch] dark:text-white/88 compact:text-base compact:max-w-none dashboard-compact:sm:mt-0 dashboard-compact:sm:border-s dashboard-compact:sm:border-current/35 dashboard-compact:sm:ps-4">
        {date}
      </p>
    </div>
  );
}
