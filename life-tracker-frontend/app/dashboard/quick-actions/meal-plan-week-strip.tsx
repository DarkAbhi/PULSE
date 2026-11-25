"use client";

import { useEffect, useState } from "react";
import { useTimeZone } from "@/app/components/profile-preferences-provider";

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
    <span className="flex gap-[clamp(4px,.45cqi,6px)] dashboard-compact:grid dashboard-compact:max-w-62 dashboard-compact:grid-cols-7">
      {DAYS.map(([day, label]) => (
        <span key={day} className="grid place-items-center w-[clamp(22px,2.05cqi,26px)] aspect-square border-[1.5px] border-white/50 rounded-full text-[.72rem] font-bold data-[today=true]:border-white data-[today=true]:bg-white data-[today=true]:text-(--tile-start) dashboard-compact:w-auto dashboard-compact:min-w-0" data-today={day === today}>
          {label}
        </span>
      ))}
    </span>
  );
}
