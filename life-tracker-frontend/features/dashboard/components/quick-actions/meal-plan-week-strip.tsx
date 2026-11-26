"use client";

import { useEffect, useState } from "react";
import { useTimeZone } from "@/providers/profile-preferences-provider";

const DAYS = [
  ["Mon", "M"],
  ["Tue", "T"],
  ["Wed", "W"],
  ["Thu", "T"],
  ["Fri", "F"],
  ["Sat", "S"],
  ["Sun", "S"],
];

export default function MealPlanWeekStrip() {
  const { timeZone } = useTimeZone();
  const [today, setToday] = useState<string>();

  useEffect(() => {
    const weekday = new Intl.DateTimeFormat("en-US", { weekday: "short", timeZone });
    const update = () => setToday(weekday.format(new Date()));
    update();
    const timer = window.setInterval(update, 60_000);
    return () => window.clearInterval(timer);
  }, [timeZone]);

  return (
    <span className="flex gap-1 dashboard-compact:grid dashboard-compact:max-w-64 dashboard-compact:grid-cols-7">
      {DAYS.map(([day, label]) => (
        <span key={day} className="grid aspect-square w-6 place-items-center rounded-full border-2 border-white/40 text-caption font-bold data-today:border-white data-today:bg-white data-today:text-tile-start dashboard-compact:w-auto dashboard-compact:min-w-0" data-today={day === today || undefined}>
          {label}
        </span>
      ))}
    </span>
  );
}
